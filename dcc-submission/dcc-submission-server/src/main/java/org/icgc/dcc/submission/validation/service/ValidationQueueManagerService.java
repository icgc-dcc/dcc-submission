/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.validation.service;

import static cascading.stats.CascadingStats.Status.FAILED;
import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.release.model.SubmissionState.ERROR;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALID;
import static org.icgc.dcc.submission.validation.report.Outcome.PASSED;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.release.ReleaseService;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.validation.FilePresenceException;
import org.icgc.dcc.submission.validation.Plan;
import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.report.Outcome;
import org.icgc.dcc.submission.validation.report.SchemaReport;
import org.icgc.dcc.submission.validation.report.SubmissionReport;
import org.icgc.dcc.submission.validation.report.ValidationErrorReport;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeListener;
import cascading.stats.CascadingStats.Status;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.typesafe.config.Config;

/**
 * Manages validation queue:<br>
 * - Launches validation for queued submissions<br>
 * - Updates submission states upon termination of the validation process
 */
@Slf4j
public class ValidationQueueManagerService extends AbstractService {

  /**
   * Frequency at which the service polls for an open release and for enqueued projects there is one.
   */
  private static final int POLLING_FREQUENCY_PER_SEC = 1;

  /**
   * Default value for maximum number of concurrent validations.
   */
  private static final int DEFAULT_MAX_VALIDATING = 1;

  private static final String MAX_VALIDATING_CONFIG_PARAM = "validator.max_simultaneous";

  private static final Optional<QueuedProject> ABSENT_QUEUED_PROJECT = absent(); // TODO: SUBM-2
  private static final Optional<Throwable> ABSENT_THROWABLE = absent();

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
  private final int maxValidating;
  private final ReleaseService releaseService;
  private final ValidationService validationService;
  private final MailService mailService;

  /**
   * To keep track of parallel validations. Volatility is guaranteed according to
   * http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/atomic/package-summary.html.
   */
  private final AtomicInteger parallelValidationCount = new AtomicInteger();

  private ScheduledFuture<?> schedule;

  @Inject
  public ValidationQueueManagerService(final ReleaseService releaseService, ValidationService validationService,
      MailService mailService, Config config) {
    this.releaseService = checkNotNull(releaseService);
    this.validationService = checkNotNull(validationService);
    this.mailService = mailService;

    this.maxValidating = checkNotNull(config).hasPath(MAX_VALIDATING_CONFIG_PARAM) ?
        config.getInt(MAX_VALIDATING_CONFIG_PARAM) :
        DEFAULT_MAX_VALIDATING;
  }

  @Override
  protected void doStart() {
    startScheduler();
    notifyStarted(); // MUST happen after the above method, by contract
  }

  @Override
  protected void doStop() {
    stopScheduler();
    notifyStopped(); // MUST happen after the above method, by contract
  }

  private void startScheduler() {
    log.info("Polling queue every {} second", POLLING_FREQUENCY_PER_SEC);

    schedule = scheduler.scheduleWithFixedDelay(
        new Runnable() {

          @Override
          public void run() {
            checkState(isRunning(), "Expecting service to be running."); // Thanks to the initial delay we set

            /*
             * TODO: SUBM-1 - Ideally we wouldn't do that every time (but requires creating yet a separate thread for
             * that)
             */
            // TODO: if first time, send out email (wait for SUBM-1 to be done)
            {
              long count = waitForAnOpenRelease();
              checkState(
                  count == 1,
                  "Expecting one and only one '%s' release, instead getting '%s'",
                  OPENED, count);
            }

            pollQueue();
          }
        },
        POLLING_FREQUENCY_PER_SEC,
        POLLING_FREQUENCY_PER_SEC,
        SECONDS);
  }

  private void stopScheduler() {
    try {
      if (schedule.cancel(true)) {
        log.info("Successfully cancelled the schedule");
      } else {
        log.error("Failed to cancel schedule");
      }
    } finally {
      log.info("Shutting down scheduler");
      scheduler.shutdown();
    }
  }

  /**
   * Kicks off processing for the next project if there is one and handles potential errors.
   */
  private void pollQueue() {
    log.debug("Polling queue.");
    Optional<QueuedProject> optionalNextProject = ABSENT_QUEUED_PROJECT;
    Optional<Throwable> criticalThrowable = ABSENT_THROWABLE;
    try {
      val release = fetchOpenRelease();
      optionalNextProject = release.nextInQueue();
      if (optionalNextProject.isPresent() && hasEmptyValidationSlot()) {
        processNext(release, optionalNextProject.get());
      }
    } catch (FilePresenceException e) { // TODO: DCC-1820
      try {
        handleAbortedValidation(e);
      } catch (Throwable t) {
        log.info("Caught an unexpected {} upon trying to abort validation", t.getClass().getSimpleName());
        criticalThrowable = Optional.fromNullable(t);
      }
    } catch (Throwable t) { // exception thrown within the run method are not logged otherwise (NullPointerException
                            // for instance)
      log.info("Caught an unexpected {}", t.getClass().getSimpleName());
      criticalThrowable = Optional.fromNullable(t);
    } finally {
      /*
       * When a scheduled job throws an exception to the executor, all future runs of the job are cancelled. Thus, we
       * should never throw an exception to our executor otherwise a server restart is necessary.
       */
      if (criticalThrowable.isPresent()) {
        processCriticalThrowable(
            criticalThrowable.get(),
            optionalNextProject);
      }
    }
  }

  /**
   * Handles the next item in the queue if there are any.
   * <p>
   * Note that a checked exception {@code FilePresenceException} may be thrown and that section 14.19 of the JLS
   * guarantees that the lock will be released in that case as well.
   * 
   * @throws FilePresenceException
   */
  private void processNext(Release release, QueuedProject project) throws FilePresenceException { // TODO: DCC-1820;
    log.info("Processing next project in queue: '{}'", project);
    mailService.sendProcessingStarted(project.getKey(), project.getEmails());

    releaseService.dequeueToValidating(project);
    Plan plan = validationService.prepareValidation(release, project, new ValidationCascadeListener());
    /**
     * Note that emptying of the .validation directory happens right before launching the cascade in
     * {@link Plan#startCascade()}
     */
    validationService.startValidation(plan); // non-blocking
  }

  /**
   * Triggered by our application.
   */
  private void handleAbortedValidation(FilePresenceException e) {
    Plan plan = e.getPlan();
    if (plan.hasFileLevelErrors() == false) {
      throw new AssertionError(); // by design since this should be the condition for throwing the
                                  // FatalPlanningException
    }

    Map<String, TupleState> fileLevelErrors = plan.getFileLevelErrors();
    log.info("There are file-level errors (fatal ones):\n\t{}", fileLevelErrors);

    QueuedProject queuedProject = checkNotNull(plan.getQueuedProject());
    String projectKey = queuedProject.getKey();
    log.info("About to dequeue project key {}", projectKey);
    resolveSubmission(queuedProject, SubmissionState.INVALID);

    SubmissionReport report = new SubmissionReport();

    List<SchemaReport> schemaReports = new ArrayList<SchemaReport>();
    for (String schema : fileLevelErrors.keySet()) {
      SchemaReport schemaReport = new SchemaReport();
      Iterator<TupleState.TupleError> es = fileLevelErrors.get(schema).getErrors().iterator();
      List<ValidationErrorReport> errReport = newArrayList();
      while (es.hasNext()) {
        errReport.add(new ValidationErrorReport(es.next()));
      }
      schemaReport.addErrors(errReport);
      schemaReport.setName(schema);
      schemaReports.add(schemaReport);
    }
    report.setSchemaReports(schemaReports);
    setSubmissionReport(projectKey, report);
  }

  /**
   * Triggered by cascading (via listener on cascade).
   */
  public void handleCompletedValidation(Plan plan) {
    checkArgument(plan != null);
    QueuedProject queuedProject = plan.getQueuedProject();
    checkNotNull(queuedProject);
    String projectKey = queuedProject.getKey();
    log.info("Cascade completed for project {}", projectKey);

    Status cascadeStatus = plan.getCascade().getCascadeStats().getStatus();
    if (FAILED == cascadeStatus) { // Completion does not guarantee success (at least in current version of
                                   // cascading)
      log.error("Validation failed: cascade completed with a {} status; About to dequeue project key {}",
          cascadeStatus, projectKey);
      resolveSubmission(queuedProject, ERROR);
    } else {
      log.info("Gathering report for project {}", projectKey);
      SubmissionReport report = new SubmissionReport();
      Outcome outcome = plan.collect(report);
      log.info("Gathered report for project {}", projectKey);

      // resolving submission
      if (outcome == PASSED) {
        resolveSubmission(queuedProject, VALID);
      } else {
        resolveSubmission(queuedProject, INVALID);
      }
      setSubmissionReport(projectKey, report);

      log.info("Validation finished normally for project {}, time spent on validation is {} seconds", projectKey,
          plan.getDuration() / 1000.0);
    }
  }

  private void setSubmissionReport(String projectKey, SubmissionReport report) {
    log.info("starting report collecting on project {}", projectKey);

    Release release = fetchOpenRelease(); // TODO: should we create it here?

    Submission submission = this.releaseService.getSubmission(release.getName(), projectKey);

    submission.setReport(report);

    // persist the report to DB
    // DCC-799: Runtime type will be SubmissionReport. Static type is Object to untangle cyclic dependencies between
    // dcc-submission-server and dcc-submission-core.
    this.releaseService
        .updateSubmissionReport(release.getName(), projectKey, (SubmissionReport) submission.getReport());
    log.info("report collecting finished on project {}", projectKey);
  }

  private void processCriticalThrowable(Throwable t, Optional<QueuedProject> optionalNextProject) {
    log.error("a critical error occured while processing the validation queue", t);

    if (checkNotNull(optionalNextProject.isPresent())) {
      QueuedProject project = optionalNextProject.get();
      try {
        resolveSubmission(project, SubmissionState.ERROR);
      } catch (Throwable t2) {
        log.error("a critical error occured while attempting to dequeue project " + project.getKey(), t2);
      }
    } else {
      log.error("next project in queue not present, could not dequeue nor set submission state to {}",
          SubmissionState.ERROR);
    }
  }

  /**
   * Triggered by cascading (via listener on cascade).
   */
  public void handleUnexpectedException(QueuedProject project) {
    checkArgument(project != null);
    log.info("failed validation from unknown error - about to dequeue project key {}", project.getKey());
    resolveSubmission(project, SubmissionState.ERROR);
  }

  /**
   * Submission resolution can happen in 4 manners:<br/>
   * - 1. cascading listener's onComplete(): VALID or INVALID<br/>
   * - 2. catching of {@code FilePresenceException} (typically a missing required file): INVALID<br/>
   * - 3. cascading listener's onStopping(): ERROR<br/>
   * - 4. catching of an unknown exception: ERROR<br/>
   */
  private void resolveSubmission(QueuedProject project, SubmissionState state) {
    String key = project.getKey();
    log.info("Resolving {}", key);
    releaseService.resolve(key, state);

    decrementParallelValidationCount();
    if (project.getEmails().isEmpty() == false) {
      this.email(project, state);
    }
    log.info("Resolved {}", key);
  }

  /**
   * Determines if runnable and increments counter if it is the case.
   * <p>
   * Whether there is a spot available for validation to occur (increment and give green light) or whether the
   * submission should remain in the queue.
   * <p>
   * Counterpart operation to <code>{@link ValidationQueueManagerService#decrementParallelValidationCount()}</code>
   */
  private synchronized boolean hasEmptyValidationSlot() {
    int original = parallelValidationCount.intValue();
    if (original < maxValidating) {
      checkState(parallelValidationCount.incrementAndGet() <= maxValidating, "by design: %s",
          parallelValidationCount.intValue());
    }
    return original < parallelValidationCount.intValue();
  }

  /**
   * Decrements the parallel validation counter.
   * <p>
   * Counterpart operation to <code>{@link ValidationQueueManagerService#hasEmptyValidationSlot()}</code>
   */
  private synchronized void decrementParallelValidationCount() {
    checkState(parallelValidationCount.decrementAndGet() >= 0);
  }

  private void email(QueuedProject project, SubmissionState state) { // TODO: SUBM-5
    Release release = fetchOpenRelease();

    Set<Address> aCheck = Sets.newHashSet();

    for (String email : project.getEmails()) {
      try {
        Address a = new InternetAddress(email);
        aCheck.add(a);
      } catch (AddressException e) {
        log.error("Illegal Address: " + e);
      }
    }

    if (aCheck.isEmpty() == false) {
      mailService.sendValidated(release.getName(), project.getKey(), state, aCheck);
    }
  }

  private Release fetchOpenRelease() {
    val release = releaseService.createNextRelease().getRelease();
    checkState(release.getState() == OPENED, "Release is expected to be '%s'", OPENED);
    return release;
  }

  private long waitForAnOpenRelease() {
    long count;
    do {
      log.info("Waiting for an '{}' release to be created.", OPENED);
      sleepUninterruptibly(
          POLLING_FREQUENCY_PER_SEC,
          SECONDS);
      count = releaseService.countOpenReleases();
    } while (count == 0);
    return count;
  }

  /**
   * TODO: externalize? may be difficult because of it calls method from the enclosing class...
   */
  public class ValidationCascadeListener implements CascadeListener {

    Plan plan;

    public void setPlan(Plan plan) {
      this.plan = plan;
    }

    @Override
    public void onStarting(Cascade cascade) {
      // No-op for now, can add in external hook
      log.debug("CascadeListener onStarting");
    }

    @Override
    public void onStopping(Cascade cascade) {
      log.debug("CascadeListener onStopping");
      handleUnexpectedException(plan.getQueuedProject());
    }

    @Override
    public void onCompleted(Cascade cascade) {
      log.debug("CascadeListener onCompleted");
      handleCompletedValidation(plan);
    }

    @Override
    public boolean onThrowable(Cascade cascade, Throwable throwable) {
      log.error("CascadeListener onThrowable: {}", throwable);

      // No-op for now; false indicates that the throwable was not handled and needs to be re-thrown
      return false;
    }
  }
}
