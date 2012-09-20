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

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.filesystem.SubmissionDirectory;
import org.icgc.dcc.release.model.QueuedProject;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.validation.CascadingStrategy;
import org.icgc.dcc.validation.FatalPlanningException;
import org.icgc.dcc.validation.Plan;
import org.icgc.dcc.validation.Planner;
import org.icgc.dcc.validation.factory.CascadingStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cascading.cascade.Cascade;

import com.google.common.collect.Iterables;
import com.google.inject.Inject;

/**
 * Wraps validation call for the {@code ValidationQueueManagerService} and {@Main} (the validation one) to use
 */
public class ValidationService {

  private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

  private final Planner planner;

  private final DccFileSystem dccFileSystem;

  private final ProjectService projectService;

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
    this.projectService = projectService;
    this.planner = planner;
    this.dictionaries = dictionaries;
    this.cascadingStrategyFactory = cascadingStrategyFactory;

  }

  public Plan validate(Release release, QueuedProject qProject) {
    String dictionaryVersion = release.getDictionaryVersion();
    Dictionary dictionary = this.dictionaries.getFromVersion(dictionaryVersion);
    ReleaseFileSystem releaseFilesystem = dccFileSystem.getReleaseFilesystem(release);

    Project project = projectService.getProject(qProject.getKey());
    SubmissionDirectory submissionDirectory = releaseFilesystem.getSubmissionDirectory(project);

    Path rootDir = new Path(submissionDirectory.getSubmissionDirPath());
    Path outputDir = new Path(submissionDirectory.getValidationDirPath());
    Path systemDir = releaseFilesystem.getSystemDirectory();

    log.info("rootDir = {} ", rootDir);
    log.info("outputDir = {} ", outputDir);

    CascadingStrategy cascadingStrategy = cascadingStrategyFactory.get(rootDir, outputDir, systemDir);

    log.info("starting validation on project {}", qProject.getKey());
    Plan plan = planCascade(qProject, cascadingStrategy, dictionary);

    runCascade(plan.getCascade(), project.getKey());
    log.info("validation finished for project {}", project.getKey());

    return plan;
  }

  public Plan planCascade(QueuedProject project, CascadingStrategy cascadingStrategy, Dictionary dictionary) {

    Plan plan = planner.plan(cascadingStrategy, dictionary);

    log.info("# internal flows: {}", Iterables.size(plan.getInternalFlows()));
    log.info("# external flows: {}", Iterables.size(plan.getExternalFlows()));

    plan.connect(cascadingStrategy);

    if(plan.hasFileLevelErrors()) {
      throw new FatalPlanningException(project, plan); // the queue manager will handle it
    }

    return plan;
  }

  public void runCascade(Cascade cascade, String projectKey) {
    int size = cascade.getFlows().size();
    log.info("starting cascade with {} flows", size);
    cascade.complete();
    log.info("completed cascade with {} flows", size);
  }
}
