/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation.service;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.filesystem.SubmissionDirectory;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.validation.CascadingStrategy;
import org.icgc.dcc.validation.LocalCascadingStrategy;
import org.icgc.dcc.validation.Plan;
import org.icgc.dcc.validation.ValidationCallback;
import org.icgc.dcc.validation.report.SubmissionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;

/**
 * Manages validation queue that:<br>
 * - launches validation for queue submissions<br>
 * - updates submission states upon termination of the validation process
 */
public class ValidationQueueManagerService extends AbstractService implements ValidationCallback {

  private static final Logger log = LoggerFactory.getLogger(ValidationQueueManagerService.class);

  private static final int POLLING_FREQUENCY_PER_SEC = 1;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  private final ReleaseService releaseService;

  private final DictionaryService dictionaryService;

  private final ValidationService validationService;

  private final DccFileSystem dccFileSystem;

  private final ProjectService projectService;

  private ScheduledFuture<?> schedule;

  @Inject
  public ValidationQueueManagerService(final ReleaseService releaseService, final DictionaryService dictionaryService,
      ValidationService validationService, final DccFileSystem dccFileSystem, final ProjectService projectService) {

    checkArgument(releaseService != null);
    checkArgument(dictionaryService != null);
    checkArgument(validationService != null);

    this.releaseService = releaseService;
    this.dictionaryService = dictionaryService;
    this.validationService = validationService;
    this.dccFileSystem = dccFileSystem;
    this.projectService = projectService;
  }

  @Override
  protected void doStart() {
    notifyStarted();
    startScheduler();
  }

  @Override
  protected void doStop() {
    stopScheduler();
    notifyStopped();
  }

  private void startScheduler() {
    log.info("polling queue every {} second", POLLING_FREQUENCY_PER_SEC);
    schedule = scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        Optional<String> next = Optional.<String> absent();
        try {
          if(isRunning() && releaseService.hasNextRelease()) {
            next = releaseService.getNextRelease().getNextInQueue();
            if(next.isPresent()) {
              log.info("next in queue {}", next);
              Release release = releaseService.getNextRelease().getRelease();
              if(release == null) {
                throw new ValidationServiceException("cannot access the next release");
              } else {
                String dictionaryVersion = release.getDictionaryVersion();
                Dictionary dictionary = dictionaryService.getFromVersion(dictionaryVersion);
                if(dictionary == null) {
                  throw new ValidationServiceException(String.format("no dictionary found with version %s",
                      dictionaryVersion));
                } else {
                  String projectKey = next.get();
                  Plan plan = validationService.validate(release, projectKey);
                  if(plan.getCascade().getCascadeStats().isSuccessful()) {
                    handleSuccessfulValidation(projectKey, plan);
                  } else {
                    handleFailedValidation(projectKey);
                  }
                }
              }
            }
          }
        } catch(Exception e) { // exception thrown within the run method are not logged otherwise (NullPointerException
                               // for instance)
          log.error("an error occured while processing the validation queue", e);
          if(next.isPresent()) {
            dequeue(next.get(), false);
          }
        }
      }
    }, POLLING_FREQUENCY_PER_SEC, POLLING_FREQUENCY_PER_SEC, TimeUnit.SECONDS);
  }

  private void stopScheduler() {
    try {
      boolean cancel = schedule.cancel(true);
      log.info("attempt to cancel returned {}", cancel);
    } finally {
      scheduler.shutdown();
    }
  }

  @Override
  public void handleSuccessfulValidation(String projectKey, Plan plan) {
    checkArgument(projectKey != null);
    log.info("successful validation - about to dequeue project key {}", projectKey);
    dequeue(projectKey, true);

    log.info("starting report collecting on project {}", projectKey);

    Release release = releaseService.getNextRelease().getRelease();

    ReleaseFileSystem releaseFilesystem = dccFileSystem.getReleaseFilesystem(release);

    Project project = projectService.getProject(projectKey);
    SubmissionDirectory submissionDirectory = releaseFilesystem.getSubmissionDirectory(project);

    File rootDir = new File(submissionDirectory.getSubmissionDirPath());
    File outputDir = new File(submissionDirectory.getValidationDirPath());

    Submission submission = this.releaseService.getSubmission(release.getName(), projectKey);

    CascadingStrategy cascadingStrategy = new LocalCascadingStrategy(rootDir, outputDir);

    SubmissionReport report = new SubmissionReport();
    plan.collect(cascadingStrategy, report);
    submission.setReport(report);
    // persist the report to DB
    this.releaseService.updateSubmissionReport(release.getName(), projectKey, submission.getReport());
    log.info("report collecting finished on project {}", projectKey);
  }

  @Override
  public void handleFailedValidation(String projectKey) {
    checkArgument(projectKey != null);
    log.info("failed validation - about to dequeue project key {}", projectKey);
    dequeue(projectKey, false);
  }

  private void dequeue(String projectKey, boolean valid) {
    Optional<String> dequeuedProjectKey = releaseService.dequeue(projectKey, valid);
    if(dequeuedProjectKey.isPresent() == false) {
      log.warn("could not dequeue project {}, maybe the queue was emptied in the meantime?", projectKey);
    }
  }

}
