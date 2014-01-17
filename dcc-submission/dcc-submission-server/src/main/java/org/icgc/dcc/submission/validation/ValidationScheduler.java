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
package org.icgc.dcc.submission.validation;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.util.concurrent.AbstractScheduledService.Scheduler.newFixedDelaySchedule;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.release.model.SubmissionState.ERROR;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.NOT_VALIDATED;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALID;

import java.util.Set;
import java.util.concurrent.CancellationException;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Synchronized;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.core.model.InvalidStateException;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.release.ReleaseService;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.validation.core.DefaultValidationContext;
import org.icgc.dcc.submission.validation.core.SubmissionReport;
import org.icgc.dcc.submission.validation.core.SubmissionReportContext;
import org.icgc.dcc.submission.validation.core.Validation;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.platform.PlatformStrategyFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.inject.Inject;

/**
 * Coordinator task that runs periodically to dispatch validations for execution. It pulls from the web request "queue"
 * as input and pushes to the validation "executor" as output. Also responsible for mediating validation cancellation
 * requests coming from the web layer.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @_(@Inject))
public class ValidationScheduler extends AbstractScheduledService {

  /**
   * Period at which the service polls for an open release and for an enqueued project if there is one.
   */
  private static final int POLLING_PERIOD_SECONDS = 1;

  /**
   * Dependencies.
   */
  @NonNull
  private final ReleaseService releaseService;
  @NonNull
  private final ValidationExecutor executor;
  @NonNull
  private final MailService mailService;
  @NonNull
  private final DccFileSystem dccFileSystem;
  @NonNull
  private final PlatformStrategyFactory platformStrategyFactory;
  @NonNull
  private final Set<Validator> validators;

  /**
   * Cancels a validation that was previously started in {@link #tryValidation(Release, QueuedProject)}.
   * 
   * @param projectKey the key of the project to cancel
   * @throws InvalidStateException
   */
  public void cancelValidation(String projectKey) throws InvalidStateException {
    val cancelled = executor.cancel(projectKey);
    if (cancelled) {
      // TODO: Determine when this should / needs to be called
      log.info("Resetting database and file system state for cancelled '{}' validation...", projectKey);
      releaseService.deleteQueuedRequest(projectKey);
    }
  }

  /**
   * Creates a {@code Scheduler} instance that runs every {@link #POLLING_PERIOD_SECONDS}.
   */
  @Override
  protected Scheduler scheduler() {
    return newFixedDelaySchedule(POLLING_PERIOD_SECONDS, POLLING_PERIOD_SECONDS, SECONDS);
  }

  /**
   * Main {@code Validation} dispatch loop that that is invoked by the {@link #scheduler()}.
   * 
   * @throws Exception
   */
  @Override
  protected void runOneIteration() throws Exception {
    try {
      pollOpenRelease();
      pollQueue();
    } catch (Exception e) {
      log.error("Exception polling:", e);
      mailService.sendSupportProblem(e.getMessage(), getStackTraceAsString(e));

      // This will terminate the AbstractScheduledService thread but that is the safest thing to do here
      throw e;
    }
  }

  /**
   * Ensures that the underlying validation executor tasks are shutdown gracefully when the main shutdown hook is
   * triggered.
   * 
   * @throws Exception
   */
  @Override
  protected void shutDown() throws Exception {
    executor.shutdown();
  }

  /**
   * Polls for an open release to become available.
   * @throws InterruptedException
   */
  private void pollOpenRelease() throws InterruptedException {
    long count;
    do {
      // Allow for interruption
      sleep(MILLISECONDS.convert(POLLING_PERIOD_SECONDS, SECONDS));

      // Should almost always be 1
      count = releaseService.countOpenReleases();
    } while (count == 0);

    // This can happen during a release, see DCC-1931
    checkState(count == 1, "Expecting one and only one '%s' release, instead getting '%s'",
        OPENED, count);
  }

  /**
   * Polls for an enqueued project to become available
   */
  private void pollQueue() {
    log.debug("Polling validation queue...");
    Optional<QueuedProject> nextProject = absent();

    try {
      // Try to find a queued validation
      val release = resolveOpenRelease();
      nextProject = release.nextInQueue();

      if (nextProject.isPresent()) {
        log.info("Trying to validate next eligible project in queue: '{}'", nextProject.get());
        tryValidation(release, nextProject.get());
      }
    } catch (ValidationRejectedException e) {
      // No available slots
      log.info("Validation for '{}' was rejected:", nextProject.get());
    } catch (Throwable t) {
      log.error("Caught an unexpected exception: {}", t);
    }
  }

  /**
   * Attempts to validate an enqueued project.
   * 
   * @param release the current release
   * @param project the project to validate
   * @throws ValidationRejectedException if the validation could not be executed
   */
  private void tryValidation(final Release release, final QueuedProject project) {
    // Prepare validation
    val validation = createValidation(release, project);

    // Submit validation asynchronously for execution
    executor.execute(validation, new Runnable() {

      /**
       * Called if and when validation is accepted (asynchronously).
       */
      @Override
      public void run() {
        // If we made it here then the validation was accepted
        log.info("onAccepted - Accepting next project in queue: '{}'", project);
        acceptValidation(project, release);
        log.info("onAccepted -  '{}'", project);
      }

    }, new FutureCallback<Validation>() {

      /**
       * Called when validation has completed (may not be VALID)
       */
      @Override
      public void onSuccess(Validation validation) {
        log.info("onSuccess - Finished validation for '{}'", project.getKey());

        val state = validation.getContext().hasErrors() ? INVALID : VALID;
        val submissionReport = validation.getContext().getSubmissionReport();
        completeValidation(project, state, submissionReport);
        log.info("onSuccess - Completed '{}'", project.getKey());
      }

      /**
       * Called when validation has completed (will not be VALID)
       */
      @Override
      public void onFailure(Throwable t) {
        log.error("onFailure - Throwable occurred in '{}' validation: {}", project.getKey(), t);

        val state = t instanceof CancellationException ? NOT_VALIDATED : ERROR;
        val submissionReport = validation.getContext().getSubmissionReport();
        completeValidation(project, state, submissionReport);
        log.info("onFailure - Completed '{}'.", project.getKey());
      }

    });
  }

  /**
   * Internal {@code Validation} factory method.
   * 
   * @param release the current release
   * @param project the project to create a validation for
   * @return
   */
  private Validation createValidation(Release release, QueuedProject project) {
    val context = createValidationContext(release, project);
    val validators = ImmutableList.<Validator> copyOf(this.validators);
    val validation = new Validation(context, validators);

    return validation;
  }

  /**
   * Intenal {@code ValidationContext} factory method.
   * 
   * @param release the current release
   * @param project the project to create a validation for
   * @return
   */
  private ValidationContext createValidationContext(Release release, QueuedProject project) {
    val dictionary = releaseService.getNextDictionary();
    val context = new DefaultValidationContext(
        new SubmissionReportContext(),
        project.getKey(),
        project.getEmails(),
        release, dictionary,
        dccFileSystem,
        platformStrategyFactory);

    return context;
  }

  /**
   * Called after a successful submission to affect the queue state and notify end users that validation has begun.
   * 
   * @param project
   */
  @Synchronized
  private void acceptValidation(QueuedProject project, Release release) {
    log.info("Validation for '{}' accepted", project);
    mailService.sendValidationStarted(release.getName(), project.getKey(), project.getEmails());
    releaseService.resetValidationFolder(project.getKey(), release);
    releaseService.dequeueToValidating(project);
  }

  /**
   * Always called after validation has completed to record the submission report and update the submission's state.
   * 
   * @param project the project to complete
   * @param state completion state
   * @param submissionReport the report produced through the validation process
   */
  @Synchronized
  private void completeValidation(QueuedProject project, SubmissionState state,
      SubmissionReport submissionReport) {
    log.info("Validation for '{}' completed with state '{}'", project, state);
    try {
      storeSubmissionReport(project.getKey(), submissionReport);
    } finally {
      resolveSubmission(project, state);
    }
  }

  /**
   * Utility method to give the current "next release" object and confirms open state.
   */
  private Release resolveOpenRelease() {
    val release = releaseService.getNextRelease();
    checkState(release.getState() == OPENED, "Release is expected to be '%s'", OPENED);
    return release;
  }

  private void storeSubmissionReport(String projectKey, SubmissionReport report) {
    // Persist the report to DB
    log.info("Storing validation submission report for project '{}': {}...", projectKey, report);
    val releaseName = resolveOpenRelease().getName();
    releaseService.updateSubmissionReport(releaseName, projectKey, report);
    log.info("Finished storing validation submission report for project '{}'", projectKey);
  }

  /**
   * "Resolves" the submission of specified {@code queuedProject} to the supplied submission {@code state}.
   * 
   * @param queuedProject the project to resolve
   * @param state the destination state of the resolution
   */
  private void resolveSubmission(QueuedProject queuedProject, SubmissionState state) {
    val projectKey = queuedProject.getKey();
    log.info("Resolving project '{}' to submission state '{}'", projectKey, state);
    releaseService.resolve(projectKey, state);

    if (!queuedProject.getEmails().isEmpty()) {
      log.info("Sending notification email for project '{}'...", queuedProject);
      notifyRecipients(queuedProject, state);
    }

    log.info("Resolved project '{}'", projectKey);
  }

  /**
   * Notifies recipients by email that the validation has been "resolved".
   * 
   * @param queuedProject the project to resolve
   * @param state the destination state of the resolution
   */
  private void notifyRecipients(QueuedProject queuedProject, SubmissionState state) {
    val release = resolveOpenRelease();

    mailService.sendValidationResult(release.getName(), queuedProject.getKey(), queuedProject.getEmails(), state);
  }

}
