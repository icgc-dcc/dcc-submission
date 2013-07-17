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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.core.ProjectService;
import org.icgc.dcc.submission.dictionary.DictionaryService;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.CascadingStrategy;
import org.icgc.dcc.submission.validation.FilePresenceException;
import org.icgc.dcc.submission.validation.Plan;
import org.icgc.dcc.submission.validation.Planner;
import org.icgc.dcc.submission.validation.factory.CascadingStrategyFactory;
import org.icgc.dcc.submission.validation.service.ValidationQueueManagerService.ValidationCascadeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeListener;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

/**
 * Wraps validation call for the {@code ValidationQueueManagerService} and {@Main} (the validation one) to use
 */
public class ValidationService {

  private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

  private final Planner planner;

  private final DccFileSystem dccFileSystem;

  private final DictionaryService dictionaries;

  private final CascadingStrategyFactory cascadingStrategyFactory;

  @Inject
  public ValidationService(final DccFileSystem dccFileSystem, final ProjectService projectService,
      final Planner planner, final DictionaryService dictionaries,
      final CascadingStrategyFactory cascadingStrategyFactory) {

    checkArgument(dccFileSystem != null);
    checkArgument(projectService != null);
    checkArgument(planner != null);
    checkArgument(dictionaries != null);
    checkArgument(cascadingStrategyFactory != null);

    this.dccFileSystem = dccFileSystem;
    this.planner = planner;
    this.dictionaries = dictionaries;
    this.cascadingStrategyFactory = cascadingStrategyFactory;

  }

  Plan prepareValidation(final Release release, final QueuedProject qProject,
      final ValidationCascadeListener validationCascadeListener) throws FilePresenceException {

    String dictionaryVersion = release.getDictionaryVersion();
    Dictionary dictionary = this.dictionaries.getFromVersion(dictionaryVersion);
    if(dictionary == null) {
      throw new ValidationServiceException(format("no dictionary found with version %s, in release %s",
          dictionaryVersion, release.getName()));
    } else {
      log.info("Preparing cascade for project {}", qProject.getKey());

      ReleaseFileSystem releaseFilesystem = dccFileSystem.getReleaseFilesystem(release);

      SubmissionDirectory submissionDirectory = releaseFilesystem.getSubmissionDirectory(qProject.getKey());

      Path rootDir = submissionDirectory.getSubmissionDirPath();
      Path outputDir = new Path(submissionDirectory.getValidationDirPath());
      Path systemDir = releaseFilesystem.getSystemDirectory();

      log.info("rootDir = {} ", rootDir);
      log.info("outputDir = {} ", outputDir);
      log.info("systemDir = {} ", systemDir);

      CascadingStrategy cascadingStrategy = cascadingStrategyFactory.get(rootDir, outputDir, systemDir);
      Plan plan =
          planAndConnectCascade(qProject, submissionDirectory, cascadingStrategy, dictionary, validationCascadeListener);

      validationCascadeListener.setPlan(plan);

      log.info("Prepared cascade for project {}", qProject.getKey());
      return plan;
    }
  }

  /**
   * Plans and connects the cascade running the validation.
   * <p>
   * Note that emptying of the .validation dir happens right before launching the cascade in {@link Plan#startCascade()}
   */
  @VisibleForTesting
  public Plan planAndConnectCascade(QueuedProject queuedProject, SubmissionDirectory submissionDirectory,
      CascadingStrategy cascadingStrategy, Dictionary dictionary, final CascadeListener cascadeListener)
      throws FilePresenceException { // TODO: separate
                                     // plan and connect?

    log.info("Planning cascade for project {}", queuedProject.getKey());
    Plan plan = planner.plan(queuedProject, submissionDirectory, cascadingStrategy, dictionary);
    log.info("Planned cascade for project {}", queuedProject.getKey());

    log.info("# internal flows: {}", Iterables.size(plan.getInternalFlows()));
    log.info("# external flows: {}", Iterables.size(plan.getExternalFlows()));

    log.info("Connecting cascade for project {}", queuedProject.getKey());
    plan.connect(cascadingStrategy);
    log.info("Connected cascade for project {}", queuedProject.getKey());
    if(plan.hasFileLevelErrors()) { // determined during connection
      log.info(String.format("plan has errors, throwing a %s", FilePresenceException.class.getSimpleName()));
      throw new FilePresenceException(plan); // the queue manager will handle it
    }

    return plan.addCascaddeListener(cascadeListener, queuedProject);
  }

  /**
   * Starts validation in a asynchronous manner.
   * <p>
   * {@code Plan} contains the {@code Cascade}.<br/>
   * This is a non-blocking call, completion is handled by
   * <code>{@link ValidationCascadeListener#onCompleted(Cascade)}</code>
   */
  void startValidation(Plan plan) {
    QueuedProject queuedProject = plan.getQueuedProject();
    checkNotNull(queuedProject);
    String projectKey = queuedProject.getKey();
    log.info("starting validation on project {}", projectKey);
    plan.startCascade();
  }
}
