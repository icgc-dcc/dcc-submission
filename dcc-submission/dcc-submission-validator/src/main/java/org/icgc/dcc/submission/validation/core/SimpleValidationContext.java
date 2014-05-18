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
package org.icgc.dcc.submission.validation.core;

import static com.typesafe.config.ConfigFactory.parseMap;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;
import static org.apache.hadoop.fs.Path.SEPARATOR;
import static org.icgc.dcc.submission.fs.FsConfig.FS_URL;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.util.ArtifactoryDictionaryResolver;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.platform.PlatformStrategyFactoryProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;

@RequiredArgsConstructor
public class SimpleValidationContext extends AbstractValidationContext {

  @NonNull
  protected final String releaseName;
  @NonNull
  protected final String projectKey;

  @NonNull
  protected final String fsRoot;
  @NonNull
  protected final String fsUrl;
  @NonNull
  protected final String jobTracker;

  @Override
  public PlatformStrategy getPlatformStrategy() {
    val provider = new PlatformStrategyFactoryProvider(getConfig(), getFileSystem());
    val factory = provider.get();

    // Reuse primary validation component
    val project = new Path(fsRoot, new Path(releaseName, projectKey));
    val input = project;
    val output = project;
    val system = new Path(SEPARATOR);
    return factory.get(input, output, system);
  }

  @Override
  public String getProjectKey() {
    return projectKey;
  }

  @Override
  public Release getRelease() {
    return new Release(releaseName);
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
  @SneakyThrows
  public FileSystem getFileSystem() {
    return FileSystem.get(getConfiguration());
  }

  @Override
  public DccFileSystem getDccFileSystem() {
    return new DccFileSystem(getConfig(), getFileSystem());
  }

  @Override
  public ReleaseFileSystem getReleaseFileSystem() {
    return new ReleaseFileSystem(getDccFileSystem(), getRelease());
  }

  @Override
  public SubmissionDirectory getSubmissionDirectory() {
    return new SubmissionDirectory(
        getDccFileSystem(), getReleaseFileSystem(), getRelease(), getProjectKey(), getSubmission());
  }

  @Override
  public Report getReport() {
    throw new UnsupportedOperationException();
  }

  private Config getConfig() {
    return parseMap(ImmutableMap.<String, Object> of(
        "hadoop.mapred.job.tracker", jobTracker,
        "hadoop.fs.defaultFS", fsUrl,

        "fs.root", fsRoot,
        "fs.url", fsUrl
        ));
  }

  private Configuration getConfiguration() {
    val fsUrl = getConfig().getString(FS_URL);
    val configuration = new Configuration();
    configuration.set(FS_DEFAULT_NAME_KEY, fsUrl);

    return configuration;
  }

  private Submission getSubmission() {
    val projectName = getProjectKey();
    return new Submission(getProjectKey(), projectName, releaseName);
  }

}