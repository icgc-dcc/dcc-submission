/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
import static com.typesafe.config.ConfigFactory.parseMap;
import static org.apache.hadoop.fs.Path.SEPARATOR;
import static org.icgc.dcc.core.model.Configurations.HADOOP_KEY;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.SSM_TYPE;
import static org.icgc.dcc.core.util.FsConfig.FS_ROOT;
import static org.icgc.dcc.core.util.FsConfig.FS_URL;
import static org.icgc.dcc.core.util.Joiners.DOT;
import static org.icgc.dcc.hadoop.util.HadoopConstants.FS_DEFAULT_FS;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MR_JOBTRACKER_ADDRESS_KEY;

import java.util.Collection;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.core.util.ArtifactoryDictionaryResolver;
import org.icgc.dcc.hadoop.fs.FileSystems;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.validation.core.AbstractValidationContext;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.platform.PlatformStrategyFactoryProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;

public class NomalizationValidationContext extends AbstractValidationContext {

  @Value
  class Param {

    String fsUrl;
    String fsRoot;
    String jobTracker;

    private final Config getAppConfig() {
      return parseMap(ImmutableMap.<String, Object> of(
          DOT.join(HADOOP_KEY, MR_JOBTRACKER_ADDRESS_KEY), jobTracker,
          DOT.join(HADOOP_KEY, FS_DEFAULT_FS), fsUrl,
          FS_URL, fsUrl,
          FS_ROOT, fsRoot
          ));
    }

  }

  @NonNull
  private final String overarchDirName;
  @NonNull
  private final String parentInputDirName;
  @NonNull
  private final String projectKey;

  @NonNull
  private final Param param;

  @Getter
  @NonNull
  private final Release release;
  @Getter
  @NonNull
  private final Submission submission;
  @Getter
  @NonNull
  private final FileSystem fileSystem;
  @Getter
  @NonNull
  private final DccFileSystem dccFileSystem;
  @Getter
  @NonNull
  private final ReleaseFileSystem releaseFileSystem;
  @Getter
  @NonNull
  private final SubmissionDirectory submissionDirectory;

  public NomalizationValidationContext(
      String overarchDirName, String parentInputDirName, String projectKey, String fsUrl, String jobTracker) {
    this.overarchDirName = overarchDirName;
    this.parentInputDirName = parentInputDirName;
    this.projectKey = projectKey;

    this.param = new Param(fsUrl, overarchDirName, jobTracker);

    this.release = new Release(parentInputDirName);
    this.submission = new Submission(projectKey, projectKey, parentInputDirName);

    this.fileSystem = FileSystems.getFileSystem(fsUrl);
    this.dccFileSystem = new DccFileSystem(param.getAppConfig(), fileSystem);
    this.releaseFileSystem = new ReleaseFileSystem(dccFileSystem, release);
    this.submissionDirectory = new SubmissionDirectory(
        dccFileSystem, releaseFileSystem, release, projectKey, submission);
  }

  @Override
  public PlatformStrategy getPlatformStrategy() {
    val provider = new PlatformStrategyFactoryProvider(param.getAppConfig(), getFileSystem());
    val factory = provider.get();

    // Reuse primary validation component
    val project = new Path(overarchDirName, new Path(parentInputDirName, projectKey));
    val input = project;
    val output = project;
    val system = new Path(SEPARATOR); // Not used by normalizer
    return factory.get(input, output, system);
  }

  @Override
  @SneakyThrows
  public Dictionary getDictionary() {
    // Deserialize
    val objectNode = new ArtifactoryDictionaryResolver().getDictionary(DICTIONARY_VERSION);
    val reader = new ObjectMapper().reader(Dictionary.class);
    Dictionary dictionary = reader.readValue(objectNode);

    return dictionary;
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

}