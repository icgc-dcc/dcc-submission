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
package org.icgc.dcc.submission.validation.core;

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SSM_S_TYPE;
import static org.icgc.dcc.submission.dictionary.util.Dictionaries.readResourcesFileSchema;

import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.DataType.DataTypes;
import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.util.Dictionaries;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.fs.SubmissionFileSystem;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategyFactoryProvider;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BasicValidationContext extends AbstractValidationContext {

  @NonNull
  @Getter
  private final String releaseName;
  @NonNull
  @Getter
  private final String projectKey;
  @NonNull
  @Getter
  private final String fsRoot;
  @NonNull
  @Getter
  private final String fsUrl;
  @NonNull
  @Getter
  private final String jobTracker;

  @Getter(lazy = true)
  private final SubmissionProperties properties = createProperties();

  @Getter(lazy = true)
  private final List<DataType> dataTypes = DataTypes.values();
  @Getter(lazy = true)
  private final Dictionary dictionary = createDictionary();
  @Getter(lazy = true)
  private final List<CodeList> codeLists = createCodeLists();
  @Getter(lazy = true)
  private final Release release = new Release(releaseName);
  @Getter(lazy = true)
  private final SubmissionDirectory submissionDirectory = createSubmissionDirectory();
  @Getter(lazy = true)
  private final FileSystem fileSystem = createFileSystem();
  @Getter(lazy = true)
  private final SubmissionFileSystem submissionFileSystem = new SubmissionFileSystem(getProperties(), getFileSystem());
  @Getter(lazy = true)
  private final ReleaseFileSystem releaseFileSystem = new ReleaseFileSystem(getSubmissionFileSystem(), getRelease());
  @Getter(lazy = true)
  private final SubmissionPlatformStrategy platformStrategy = createPlatformStrategy();

  public BasicValidationContext(String releaseName, String projectKey, String fsRoot, String fsUrl) {
    this(releaseName, projectKey, fsRoot, fsUrl, "localhost");
  }

  @SneakyThrows
  protected Dictionary createDictionary() {
    // val dictionary = Dictionaries.readResourcesDictionary("0.11c");
    val dictionary = Dictionaries.readResourcesDictionary();

    // Add file schemata
    dictionary.addFile(readResourcesFileSchema(SSM_S_TYPE));

    return dictionary;
  }

  @SneakyThrows
  protected List<CodeList> createCodeLists() {
    return Dictionaries.readResourcesCodeLists();
  }

  @SneakyThrows
  private FileSystem createFileSystem() {
    val fsUrl = getProperties().getFs().getUrl();
    val configuration = new Configuration();
    configuration.set(FS_DEFAULT_NAME_KEY, fsUrl);

    return FileSystem.get(configuration);
  }

  private SubmissionProperties createProperties() {
    val properties = new SubmissionProperties();
    properties.getFs().setRoot(fsRoot);
    properties.getFs().setUrl(fsUrl);
    properties.getHadoop().getProperties().put("mapred.job.tracker", jobTracker);
    properties.getHadoop().getProperties().put("fs.defaultFS", fsUrl);

    return properties;
  }

  private SubmissionPlatformStrategy createPlatformStrategy() {
    val provider =
        new SubmissionPlatformStrategyFactoryProvider(getProperties(), getFileSystem());
    val factory = provider.get();

    // Reuse primary validation component
    val dummy = new Path("/");
    return factory.get(dummy, dummy);
  }

  private SubmissionDirectory createSubmissionDirectory() {
    return new SubmissionDirectory(
        getSubmissionFileSystem(),
        getReleaseFileSystem(),
        getRelease(),
        getProjectKey(),
        new Submission(projectKey, projectKey, releaseName));
  }

  @Override
  public String getOutputDirPath() {
    throw new UnsupportedOperationException("See DCC-2431");
  }

}