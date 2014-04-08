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

import static com.typesafe.config.ConfigFactory.parseMap;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;
import static org.apache.hadoop.fs.Path.SEPARATOR;
import static org.icgc.dcc.submission.fs.FsConfig.FS_URL;

import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
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

  @NonNull
  private final String releaseName;
  @NonNull
  private final String projectKey;

  @NonNull
  private final String fsRoot;
  @NonNull
  private final String fsUrl;
  @NonNull
  private final String jobTracker;

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

  @SneakyThrows
  public NomalizationValidationContext(
      String releaseName, String projectKey, String fsRoot, String fsUrl, String jobTracker) {
    this.releaseName = releaseName;
    this.projectKey = projectKey;
    this.fsRoot = fsRoot;
    this.fsUrl = fsUrl;
    this.jobTracker = jobTracker;

    this.release = new Release(releaseName);
    this.submission = new Submission(projectKey, projectKey, releaseName);

    val config = getHadoopConfig();
    this.fileSystem = FileSystem.get(config);
    this.dccFileSystem = new DccFileSystem(getAppConfig(), fileSystem);
    this.releaseFileSystem = new ReleaseFileSystem(dccFileSystem, release);
    this.submissionDirectory = new SubmissionDirectory(
        dccFileSystem, releaseFileSystem, release, projectKey, submission);
  }

  private final Configuration getHadoopConfig() {
    val fsUrl = getAppConfig().getString(FS_URL);
    val configuration = new Configuration();
    configuration.set(FS_DEFAULT_NAME_KEY, fsUrl);

    return configuration;
  }

  private final Config getAppConfig() {
    return parseMap(ImmutableMap.<String, Object> of(
        "hadoop.mapred.job.tracker", jobTracker,
        "hadoop.fs.defaultFS", fsUrl,

        "fs.root", fsRoot,
        "fs.url", fsUrl
        ));
  }

  @Override
  public PlatformStrategy getPlatformStrategy() {
    val provider = new PlatformStrategyFactoryProvider(getAppConfig(), getFileSystem());
    val factory = provider.get();

    // Reuse primary validation component
    val project = new Path(fsRoot, new Path(releaseName, projectKey));
    val input = project;
    val output = project;
    val system = new Path(SEPARATOR); // Not used by normalizer
    return factory.get(input, output, system);
  }

  @Override
  @SneakyThrows
  public Dictionary getDictionary() {
    // Resolve
    val entryName = "org/icgc/dcc/resources/Dictionary.json";
    URL url = getDictionaryUrl(DICTIONARY_VERSION);
    @Cleanup
    val zip = new ZipInputStream(url.openStream());
    ZipEntry entry;

    do {
      entry = zip.getNextEntry();
    } while (!entryName.equals(entry.getName()));

    // Deserialize
    val reader = new ObjectMapper().reader(Dictionary.class);
    Dictionary dictionary = reader.readValue(zip);

    return dictionary;
  }

  @Override
  public Report getReport() {
    throw new UnsupportedOperationException();
  }

}