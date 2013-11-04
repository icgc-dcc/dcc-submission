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
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.util.concurrent.AbstractScheduledService.Scheduler.newFixedDelaySchedule;
import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.release.model.SubmissionState.ERROR;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALID;

import java.util.Set;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.core.model.InvalidStateException;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.release.ReleaseService;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.validation.platform.PlatformStrategyFactory;
import org.icgc.dcc.submission.validation.report.SubmissionReport;
import org.icgc.dcc.submission.validation.report.SubmissionReportContext;
import org.icgc.dcc.submission.validation.service.DefaultValidationContext;
import org.icgc.dcc.submission.validation.service.Validation;
import org.icgc.dcc.submission.validation.service.ValidationContext;
import org.icgc.dcc.submission.validation.service.ValidationExecutor;
import org.icgc.dcc.submission.validation.service.ValidationRejectedException;
import org.icgc.dcc.submission.validation.service.Validator;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.FutureCallback;
import com.google.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor = @_(@Inject))
public class ValidationScheduler extends AbstractScheduledService {

  /**
   * Period at which the service polls for an open release and for enqueued projects there is one.
   */
  private static final int POLLING_PERIOD_SECONDS = 1;

  /**
   * Dependencies.
   */
  @NonNull
  private final ReleaseService releaseService;
  @NonNull
  private final ValidationExecutor validationExecutor;
  @NonNull
  private final MailService mailService;
  @NonNull
  private final DccFileSystem dccFileSystem;
  @NonNull
  private final PlatformStrategyFactory platformStrategyFactory;
  @NonNull
  private final Set<Validator> validators;

  public void cancelValidation(String projectKey) throws InvalidStateException {
    val cancelled = validationExecutor.cancel(projectKey);
    if (cancelled) {
      // TODO: Determine when this shouled be called
      log.info("Resetting database and file system state for killed project validation: {}...", projectKey);
      releaseService.deleteQueuedRequest(projectKey);
    }
  }

  @Override
  protected void runOneIteration() throws Exception {
    pollOpenRelease();
    pollQueue();
  }

  @Override
  protected Scheduler scheduler() {
    return newFixedDelaySchedule(POLLING_PERIOD_SECONDS, POLLING_PERIOD_SECONDS, SECONDS);
  }

  private void pollOpenRelease() {
    long count;
    do {
      sleepUninterruptibly(POLLING_PERIOD_SECONDS, SECONDS);
      count = releaseService.countOpenReleases();
    } while (count == 0);

    checkState(count == 1, "Expecting one and only one '%s' release, instead getting '%s'",
        OPENED, count);
  }

  private void pollQueue() {
    log.debug("Polling queue...");
    Optional<QueuedProject> nextProject = absent();

    try {
      val release = resolveOpenRelease();
      nextProject = release.nextInQueue();

      if (nextProject.isPresent()) {
        log.info("Trying to validate next eligible project in queue: '{}'", nextProject.get());
        tryValidation(release, nextProject.get());
      }
    } catch (ValidationRejectedException e) {
      log.info("Valdiation for '{}' was rejected:", nextProject.get());
    } catch (Throwable t) {
      log.error("Caught an unexpected exception: {}", t);
    }
  }

  private void tryValidation(Release release, final QueuedProject project) {
    // Prepare validation
    val validation = createValidation(release, project);

    // Submit validation asynchronously for execution
    val future = validationExecutor.execute(validation);

    // If we made it here then the validation was accepted
    log.info("Validating next project in queue: '{}'", project);
    mailService.sendProcessingStarted(project.getKey(), project.getEmails());
    releaseService.dequeueToValidating(project);

    // Add callbacks to handle execution outcomes
    addCallback(future, new FutureCallback<Validation>() {

      @Override
      public void onSuccess(Validation validation) {
        log.info("Finished validation for '{}'", project.getKey());

        try {
          storeSubmissionReport(project.getKey(), validation.getContext().getSubmissionReport());
        } finally {
          resolveSubmission(project, validation.getContext().hasErrors() ? INVALID : VALID);
        }
      }

      @Override
      public void onFailure(Throwable t) {
        log.error("Exception occurred in '{}' validation: {}", project.getKey(), t);

        try {
          storeSubmissionReport(project.getKey(), null);
        } finally {
          resolveSubmission(project, ERROR);
        }
      }

    });
  }

  private Validation createValidation(Release release, QueuedProject project) {
    val validators = ImmutableList.<Validator> copyOf(this.validators);
    val context = createValidationContext(release, project);
    val validation = new Validation(context, validators);

    return validation;
  }

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

  private Release resolveOpenRelease() {
    val release = releaseService.resolveNextRelease().getRelease();
    checkState(release.getState() == OPENED, "Release is expected to be '%s'", OPENED);
    return release;
  }

  private void storeSubmissionReport(String projectKey, SubmissionReport report) {
    // Persist the report to DB
    log.info("Storing validation submission report for project '{}'...", projectKey);
    val releaseName = resolveOpenRelease().getName();
    releaseService.updateSubmissionReport(releaseName, projectKey, report);
    log.info("Finished storing validation submission report for project '{}'", projectKey);
  }

  private void resolveSubmission(QueuedProject queuedProject, SubmissionState state) {
    val projectKey = queuedProject.getKey();
    log.info("Resolving project '{}' to submission state '{}'", projectKey, state);
    releaseService.resolve(projectKey, state);

    if (!queuedProject.getEmails().isEmpty()) {
      log.info("Sending notification email for project '{}'...", queuedProject);
      notifyRecipients(queuedProject, state);
    }

    log.info("Resolved {}", projectKey);
  }

  private void notifyRecipients(QueuedProject project, SubmissionState state) {
    val release = resolveOpenRelease();

    Set<Address> addresses = newHashSet();
    for (val email : project.getEmails()) {
      try {
        val address = new InternetAddress(email);
        addresses.add(address);
      } catch (AddressException e) {
        log.error("Illegal Address: " + e);
      }
    }

    if (!addresses.isEmpty()) {
      mailService.sendValidated(release.getName(), project.getKey(), state, addresses);
    }
  }

}
