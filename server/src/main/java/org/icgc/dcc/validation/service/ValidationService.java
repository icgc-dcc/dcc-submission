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

import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.filesystem.SubmissionDirectory;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.validation.CascadingStrategy;
import org.icgc.dcc.validation.FileSchemaDirectory;
import org.icgc.dcc.validation.LocalCascadingStrategy;
import org.icgc.dcc.validation.LocalFileSchemaDirectory;
import org.icgc.dcc.validation.Plan;
import org.icgc.dcc.validation.Planner;
import org.icgc.dcc.validation.ValidationCallback;
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

  private Plan plan;

  private final DccFileSystem dccFileSystem;

  private final ProjectService projectService;

  private final DictionaryService dictionaries;

  @Inject
  public ValidationService(final DccFileSystem dccFileSystem, final ProjectService projectService,
      final Planner planner, final DictionaryService dictionaries) {
    checkArgument(dccFileSystem != null);
    checkArgument(projectService != null);
    checkArgument(planner != null);
    checkArgument(dictionaries != null);

    this.dccFileSystem = dccFileSystem;
    this.projectService = projectService;
    this.planner = planner;
    this.dictionaries = dictionaries;
  }

  public void validate(Release release, String projectKey) {
    this.validate(release, projectKey, null); // won't change submission state afterwards if not
                                              // callback
  }

  public void validate(Release release, String projectKey, ValidationCallback validationCallback) {
    String dictionaryVersion = release.getDictionaryVersion();
    Dictionary dictionary = this.dictionaries.getFromVersion(dictionaryVersion);
    if(dictionary != null) {
      ReleaseFileSystem releaseFilesystem = dccFileSystem.getReleaseFilesystem(release);

      Project project = projectService.getProject(projectKey);
      SubmissionDirectory submissionDirectory = releaseFilesystem.getSubmissionDirectory(project);

      File rootDir = new File(submissionDirectory.getSubmissionDirPath());
      File outputDir = new File(submissionDirectory.getValidationDirPath());

      log.info("rootDir = {} ", rootDir);
      log.info("outputDir = {} ", outputDir);

      FileSchemaDirectory fileSchemaDirectory = new LocalFileSchemaDirectory(rootDir);
      CascadingStrategy cascadingStrategy = new LocalCascadingStrategy(rootDir, outputDir);

      log.info("starting validation on project {}", projectKey);
      Cascade cascade = planCascade(validationCallback, projectKey, fileSchemaDirectory, cascadingStrategy, dictionary);
      runCascade(cascade, validationCallback, projectKey);
      log.info("validation finished for project {}", projectKey);

    } else {
      log.info("there is no dictionary with version {}", dictionaryVersion);
    }
  }

  @SuppressWarnings("rawtypes")
  public Cascade planCascade(ValidationCallback validationCallback, String projectKey,
      FileSchemaDirectory fileSchemaDirectory, CascadingStrategy cascadingStrategy, Dictionary dictionary) {
    this.plan = planner.plan(fileSchemaDirectory, dictionary);
    log.info("# internal flows: {}", Iterables.size(plan.getInternalFlows()));
    log.info("# external flows: {}", Iterables.size(plan.getExternalFlows()));

    Cascade cascade = plan.connect(cascadingStrategy);
    /*
     * if(validationCallback != null) { List<Flow> flows = cascade.getFlows(); for(Flow flow : flows) {
     * ValidationFlowListener listener = new ValidationFlowListener(validationCallback, flows, projectKey, this.plan);
     * flow.addListener(listener);// TODO: once a cascade listener is available, use it instead } }
     */
    return cascade;
  }

  public void runCascade(Cascade cascade, ValidationCallback validationCallback, String projectKey) {
    int size = cascade.getFlows().size();
    if(size > 0) {
      log.info("starting cascade with {} flows", size);
      cascade.complete();
      log.info("completed cascade with {} flows", size);
      if(cascade.getCascadeStats().isSuccessful()) {
        validationCallback.handleSuccessfulValidation(projectKey, this.plan);
      } else {
        validationCallback.handleFailedValidation(projectKey);
      }
    } else {
      log.info("no flows to run");
      if(validationCallback != null) {
        validationCallback.handleSuccessfulValidation(projectKey, this.plan);
      }
    }
  }
}
