/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.norm.cli;

import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.common.core.dcc.Component.CONCATENATOR;
import static org.icgc.dcc.common.core.dcc.Component.NORMALIZER;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.SSM_TYPE;
import static org.icgc.dcc.common.core.util.Joiners.PATH;

import java.util.Collection;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.hadoop.fs.FileSystems;
import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.util.Dictionaries;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.fs.SubmissionFileSystem;
import org.icgc.dcc.submission.release.model.ReleaseSubmissionView;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.validation.core.AbstractValidationContext;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategyFactoryProvider;

@Slf4j
public class StandAloneNomalizationValidationContext extends AbstractValidationContext {

  @Value
  class Param {

    String fsUrl;
    String fsRoot;
    String jobTracker;

    private final SubmissionProperties getProperties() {
      val properties = new SubmissionProperties();
      properties.getFs().setRoot(fsRoot);
      properties.getFs().setUrl(fsUrl);
      properties.getHadoop().getProperties().put("mapred.job.tracker", jobTracker);
      properties.getHadoop().getProperties().put("fs.defaultFS", fsUrl);

      return properties;
    }

  }

  @NonNull
  private final String overarchDirName;
  @NonNull
  private final String projectKey;

  @NonNull
  private final Param param;

  @Getter
  @NonNull
  private final ReleaseSubmissionView release;
  @Getter
  @NonNull
  private final Submission submission;
  @Getter
  @NonNull
  private final FileSystem fileSystem;
  @Getter
  @NonNull
  private final SubmissionFileSystem submissionFileSystem;
  @Getter
  @NonNull
  private final ReleaseFileSystem releaseFileSystem;
  @Getter
  @NonNull
  private final SubmissionDirectory submissionDirectory;

  public StandAloneNomalizationValidationContext(
      String overarchDirName, String projectKey, String fsUrl, String jobTracker) {
    this.overarchDirName = overarchDirName;
    this.projectKey = projectKey;

    this.param = new Param(fsUrl, overarchDirName, jobTracker);

    this.release = new ReleaseSubmissionView(getFakeInputReleaseName());
    this.submission = new Submission(projectKey, projectKey, getFakeInputReleaseName());

    this.fileSystem = FileSystems.getFileSystem(fsUrl);
    this.submissionFileSystem = new SubmissionFileSystem(param.getProperties(), fileSystem);
    this.releaseFileSystem = new ReleaseFileSystem(submissionFileSystem, release);
    this.submissionDirectory = new SubmissionDirectory(
        submissionFileSystem, releaseFileSystem, release, projectKey, submission);
  }

  @Override
  public String getOutputDirPath() {
    return PATH.join(overarchDirName, getFakeOutputReleaseName(), projectKey);
  }

  @Override
  public SubmissionPlatformStrategy getPlatformStrategy() {
    val properties = param.getProperties();
    log.info("Properties: {}", properties);

    val provider =
        new SubmissionPlatformStrategyFactoryProvider(properties, getFileSystem());
    val factory = provider.get();

    // Reuse primary validation component
    val input = new Path(overarchDirName, new Path(getFakeInputReleaseName(), projectKey));
    val output = new Path(overarchDirName, new Path(getFakeOutputReleaseName(), projectKey));
    return factory.get(input, output);
  }

  @Override
  @SneakyThrows
  public Dictionary getDictionary() {
    return Dictionaries.readResourcesDictionary("0.11c");
  }

  @Override
  @SneakyThrows
  public List<CodeList> getCodeLists() {
    return Dictionaries.readResourcesCodeLists();
  }

  @Override
  public Report getReport() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<DataType> getDataTypes() {
    return newArrayList((DataType) SSM_TYPE);
  }

  @Override
  public String getProjectKey() {
    return projectKey;
  }

  /**
   * Hack: see DCC-2431.
   */
  public static String getFakeInputReleaseName() {
    return CONCATENATOR.getDirName();
  }

  /**
   * Hack: see DCC-2431.
   */
  public static String getFakeOutputReleaseName() {
    return NORMALIZER.getDirName();
  }

}