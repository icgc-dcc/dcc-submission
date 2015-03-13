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
package org.icgc.dcc.submission.service;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.getStackTraceAsString;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.util.concurrent.AbstractScheduledService.Scheduler.newFixedDelaySchedule;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.icgc.dcc.submission.core.model.Outcome.ABORTED;
import static org.icgc.dcc.submission.core.model.Outcome.CANCELLED;
import static org.icgc.dcc.submission.core.model.Outcome.COMPLETED;
import static org.icgc.dcc.submission.core.model.Outcome.FAILED;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;

import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.model.Identifiable.Identifiables;
import org.icgc.dcc.submission.core.InvalidStateException;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.ValidationExecutor;
import org.icgc.dcc.submission.validation.ValidationListener;
import org.icgc.dcc.submission.validation.ValidationRejectedException;
import org.icgc.dcc.submission.validation.core.DefaultReportContext;
import org.icgc.dcc.submission.validation.core.DefaultValidationContext;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.core.Validation;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategyFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;

/**
 * Coordinator task that runs periodically to dispatch validations for execution.
 * <p>
 * The scheduler pulls from the web request "queue" as input and pushes to the validation "executor" as output. Also
 * responsible for mediating validation cancellation requests coming from the web layer.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ValidationService extends AbstractScheduledService {

  /**
   * Period at which the service polls for an open release and for an enqueued project if there is one.
   */
  private static final int POLLING_PERIOD_SECONDS = 5;

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
  private final SubmissionPlatformStrategyFactory platformStrategyFactory;
  @NonNull
  private final Set<Validator> validators;

  /**
   * SubmissionMain {@code Validation} dispatch processing.
   * 
   * @throws Exception
   */
  public void pollValidation() throws Exception {
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
   * Cancels a validation that was previously started in {@link #tryValidation(Release, QueuedProject)}.
   * 
   * @param projectKey the key of the project to cancel
   * @throws InvalidStateException
   */
  public void cancelValidation(@NonNull String projectKey) throws InvalidStateException {
    executor.cancel(projectKey);
    log.info("Resetting database and file system state for cancelled '{}' validation...", projectKey);
    releaseService.removeQueuedSubmissions(projectKey);
  }

  /**
   * Creates a {@code Scheduler} instance that runs every {@link #POLLING_PERIOD_SECONDS}.
   */
  @Override
  protected Scheduler scheduler() {
    return newFixedDelaySchedule(POLLING_PERIOD_SECONDS, POLLING_PERIOD_SECONDS, SECONDS);
  }

  /**
   * SubmissionMain {@code Validation} dispatch loop that that is invoked by the {@link #scheduler()}.
   * 
   * @throws Exception
   */
  @Override
  protected void runOneIteration() throws Exception {
    pollValidation();
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
      val release = releaseService.getNextRelease();
      nextProject = release.nextInQueue();

      if (nextProject.isPresent()) {
        val queue = release.getQueue();
        val next = nextProject.get();
        log.info("Trying to validate next eligible project in queue: '{}' ('{}': '{}')",
            new Object[] { next.getId(), queue.size(), copyOf(transform(queue, Identifiables.getId())) });
        tryValidation(release, next);
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
  private void tryValidation(@NonNull final Release release, @NonNull final QueuedProject project) {
    // Prepare validation
    val validationContext = createValidationContext(release, project);
    val validation = createValidation(validationContext);

    // Submit validation asynchronously for execution
    executor.execute(validation, new ValidationListener() {

      /**
       * Called if and when validation is started and running.
       */
      @Override
      public void onStarted(Validation validation) {
        val newReport = validationContext.getReport();

        log.info("onStarted - Validation started for '{}'", project);
        releaseService.dequeueSubmission(project, newReport);
        log.info("onStarted - Started '{}'", project);
      }

      /**
       * Called when validation has ended without exception.
       */
      @Override
      public void onEnded(Validation validation) {
        val newReport = validationContext.getReport();
        val outcome = validation.isCompleted() ? COMPLETED : ABORTED;

        log.info("onCompletion - Validation '{}' completed with outcome '{}'", project, outcome);
        releaseService.resolveSubmission(project, outcome, newReport);
        log.info("onCompletion - Completed '{}'", project.getKey());
      }

      /**
       * Called when validation has been cancelled by the submitter.
       */
      @Override
      public void onCancelled(Validation validation) {
        val newReport = validationContext.getReport();
        val outcome = CANCELLED;

        log.warn("onCancelled - Validation '{}' completed with outcome '{}'", project, outcome);
        releaseService.resolveSubmission(project, outcome, newReport);
        log.warn("onCancelled - Completed '{}'.", project.getKey());
      }

      /**
       * Called when validation has failed due to exception.
       */
      @Override
      public void onFailure(Validation validation, Throwable t) {
        val nextReport = validationContext.getReport();
        val outcome = FAILED;

        log.error("onFailure - Throwable occurred in '{}' validation: {}", project.getKey(), t);
        releaseService.resolveSubmission(project, outcome, nextReport);
        log.error("onFailure - Completed '{}'.", project.getKey());
      }

    });
  }

  /**
   * Internal {@code Validation} factory method.
   * 
   * @param context the current validation context
   * @return
   */
  private Validation createValidation(ValidationContext context) {
    val validators = ImmutableList.<Validator> copyOf(this.validators);
    val validation = new Validation(context, validators);

    return validation;
  }

  /**
   * Internal {@code ValidationContext} factory method.
   * 
   * @param release the current release
   * @param project the project to create the validation context for
   * @return
   */
  private ValidationContext createValidationContext(Release release, QueuedProject project) {
    val dictionary = releaseService.getNextDictionary();

    val context = new DefaultValidationContext(
        createReportContext(),
        project.getKey(),
        project.getEmails(),
        project.getDataTypes(),
        release,
        dictionary,
        dccFileSystem,
        platformStrategyFactory);

    return context;
  }

  /**
   * Internal {@code ReportContext} factory method.
   */
  private static ReportContext createReportContext() {
    return new DefaultReportContext(new Report()); // Empty report will be updated then merged with existing one
  }

}
