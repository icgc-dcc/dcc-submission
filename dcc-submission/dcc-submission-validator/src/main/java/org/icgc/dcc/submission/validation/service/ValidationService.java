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

import static java.lang.String.format;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.FilePresenceException;
import org.icgc.dcc.submission.validation.core.Plan;
import org.icgc.dcc.submission.validation.core.ValidationListener;
import org.icgc.dcc.submission.validation.firstpass.FirstPassChecker;
import org.icgc.dcc.submission.validation.planner.Planner;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.platform.PlatformStrategyFactory;

import cascading.cascade.Cascade;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

/**
 * Wraps validation call for the {@code ValidationQueueService} and {@Main} (the validation one) to use
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @_(@Inject))
public class ValidationService {

  @NonNull
  private final Planner planner;
  @NonNull
  private final DccFileSystem dccFileSystem;
  @NonNull
  private final PlatformStrategyFactory platformStrategyFactory;

  public Plan prepareValidation(Release release, Dictionary dictionary, QueuedProject queuedProject,
      ValidationListener listener)
      throws FilePresenceException {
    log.info("Preparing cascade for project '{}'", queuedProject.getKey());
    val projectKey = queuedProject.getKey();
    val releaseFilesystem = dccFileSystem.getReleaseFilesystem(release);
    val submissionDirectory = releaseFilesystem.getSubmissionDirectory(projectKey);

    Path rootDir = submissionDirectory.getSubmissionDirPath();
    Path outputDir = new Path(submissionDirectory.getValidationDirPath());
    Path systemDir = releaseFilesystem.getSystemDirectory();

    log.info("Validation for '{}' has rootDir = {} ", projectKey, rootDir);
    log.info("Validation for '{}' has outputDir = {} ", projectKey, outputDir);
    log.info("Validation for '{}' has systemDir = {} ", projectKey, systemDir);

    // TODO: File Checker
    PlatformStrategy platformStrategy = platformStrategyFactory.get(rootDir, outputDir, systemDir);
    checkWellFormedness();

    Plan plan = planValidation(queuedProject, submissionDirectory, platformStrategy, dictionary, listener);
    listener.setPlan(plan);

    log.info("Prepared cascade for project {}", projectKey);
    return plan;
  }

  /**
   * Plans and connects the cascade running the validation.
   * <p>
   * Note that emptying of the .validation dir happens right before launching the cascade in {@link Plan#startCascade()}
   */
  @VisibleForTesting
  public Plan planValidation(QueuedProject queuedProject, SubmissionDirectory submissionDirectory,
      PlatformStrategy platformStategy, Dictionary dictionary, ValidationListener listener)
      throws FilePresenceException {
    // TODO: Separate plan and connect?
    val projectKey = queuedProject.getKey();
    log.info("Planning cascade for project {}...", projectKey);
    Plan plan = planner.plan(queuedProject, submissionDirectory, platformStategy, dictionary);

    log.info("Planned cascade for project {}", projectKey);
    log.info("# internal flows: {}", Iterables.size(plan.getInternalFlows()));
    log.info("# external flows: {}", Iterables.size(plan.getExternalFlows()));

    log.info("Connecting cascade for project {}", projectKey);
    plan.connect(platformStategy);
    log.info("Connected cascade for project {}", projectKey);

    if (plan.hasFileLevelErrors()) { // determined during connection
      log.info(format("Submission has file-level errors, throwing a '%s'",
          FilePresenceException.class.getSimpleName()));
      throw new FilePresenceException(plan); // the queue manager will handle it
    }

    if (listener != null) {
      plan.addCascadeListener(listener);
    }

    return plan;
  }

  /**
   * Starts validation in a asynchronous manner.
   * <p>
   * {@code Plan} contains the {@code Cascade}.<br/>
   * This is a non-blocking call, completion is handled by
   * <code>{@link ValidationCascadeListener#onCompleted(Cascade)}</code>
   */
  public void startValidation(Plan plan) {
    log.info("starting validation on project {}", plan.getProjectKey());
    plan.startCascade();

    log.info("Plan: plan.getCascade: {}", plan.getCascade());
  }

  /**
   * Temporarily and until properly re-written (DCC-1820).
   */
  private void checkWellFormedness() throws FilePresenceException {
    if (FirstPassChecker.check()) {
      // Always returns true for now
      log.info("Submission is well-formed.");
    } else {
      log.info("Submission has well-formedness problems"); // TODO: expand
      // FIXME: pass appropriate objects: offending project key and Map<String, TupleState> fileLevelErrors
      throw new FilePresenceException(null);
    }
  }

}
