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
package org.icgc.dcc.submission.validation.key.cli;

import static com.typesafe.config.ConfigFactory.parseMap;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;
import static org.icgc.dcc.core.model.FileTypes.FileType.METH_M_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.METH_P_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.METH_S_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_S_TYPE;
import static org.icgc.dcc.submission.dictionary.util.Dictionaries.readFileSchema;
import static org.icgc.dcc.submission.fs.FsConfig.FS_URL;

import java.net.URL;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.Cleanup;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.core.model.DataType.DataTypes;
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

@RequiredArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KeyValidationContext extends AbstractValidationContext {

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
  private final Config config = createConfig();
  @Getter(lazy = true)
  private final List<DataType> dataTypes = DataTypes.values();
  @Getter(lazy = true)
  private final Dictionary dictionary = createDictionary();
  @Getter(lazy = true)
  private final Release release = new Release(releaseName);
  @Getter(lazy = true)
  private final SubmissionDirectory submissionDirectory = createSubmissionDirectory();
  @Getter(lazy = true)
  private final FileSystem fileSystem = createFileSystem();
  @Getter(lazy = true)
  private final DccFileSystem dccFileSystem = new DccFileSystem(getConfig(), getFileSystem());
  @Getter(lazy = true)
  private final ReleaseFileSystem releaseFileSystem = new ReleaseFileSystem(getDccFileSystem(), getRelease());
  @Getter(lazy = true)
  private final PlatformStrategy platformStrategy = createPlatformStrategy();

  @SneakyThrows
  protected Dictionary createDictionary() {
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

    // Add file schemata
    dictionary.addFile(readFileSchema(SSM_S_TYPE));
    dictionary.addFile(readFileSchema(METH_M_TYPE));
    dictionary.addFile(readFileSchema(METH_P_TYPE));
    dictionary.addFile(readFileSchema(METH_S_TYPE));

    return dictionary;
  }

  @SneakyThrows
  private FileSystem createFileSystem() {
    val fsUrl = getConfig().getString(FS_URL);
    val configuration = new Configuration();
    configuration.set(FS_DEFAULT_NAME_KEY, fsUrl);

    return FileSystem.get(configuration);
  }

  private Config createConfig() {
    return parseMap(ImmutableMap.<String, Object> of(
        "hadoop.mapred.job.tracker", jobTracker,
        "hadoop.fs.defaultFS", fsUrl,

        "fs.root", fsRoot,
        "fs.url", fsUrl
        ));
  }

  private PlatformStrategy createPlatformStrategy() {
    val provider = new PlatformStrategyFactoryProvider(getConfig(), getFileSystem());
    val factory = provider.get();

    // Reuse primary validation component
    val dummy = new Path("/");
    return factory.get(dummy, dummy, dummy);
  }

  private SubmissionDirectory createSubmissionDirectory() {
    return new SubmissionDirectory(
        getDccFileSystem(),
        getReleaseFileSystem(),
        getRelease(),
        getProjectKey(),
        new Submission(projectKey, projectKey, releaseName));
  }

}