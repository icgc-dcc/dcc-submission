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
package org.icgc.dcc.submission.normalization;

import static com.typesafe.config.ConfigFactory.parseMap;
import static java.lang.String.format;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;
import static org.apache.hadoop.fs.Path.SEPARATOR;
import static org.icgc.dcc.submission.fs.FsConfig.FS_URL;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;
import org.icgc.dcc.submission.validation.core.ErrorType;
import org.icgc.dcc.submission.validation.core.FieldReport;
import org.icgc.dcc.submission.validation.core.SubmissionReport;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.platform.PlatformStrategyFactoryProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;

@Slf4j
@RequiredArgsConstructor()
public class NomalizationValidationContext implements ValidationContext {

  private static final String DICTIONARY_VERSION = "0.7c";

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

  @Override
  public PlatformStrategy getPlatformStrategy() {
    val provider = new PlatformStrategyFactoryProvider(getConfig(), getFileSystem());
    val factory = provider.get();

    // Reuse primary validation component
    val project = new Path(fsRoot, new Path(releaseName, projectKey));
    val input = project;
    val output = project;
    val system = new Path(SEPARATOR); // Not used by normalizer
    return factory.get(input, output, system);
  }

  @Override
  public String getProjectKey() {
    return projectKey;
  }

  @Override
  public List<String> getEmails() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Release getRelease() {
    return new Release(releaseName);
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
  public SubmissionDirectory getSubmissionDirectory() {
    return new SubmissionDirectory(getDccFileSystem(), getRelease(), getProjectKey(), getSubmission());
  }

  @Override
  public DccFileSystem getDccFileSystem() {
    return new DccFileSystem(getConfig(), getFileSystem());
  }

  @Override
  @SneakyThrows
  public FileSystem getFileSystem() {
    return FileSystem.get(getConfiguration());
  }

  @Override
  public ReleaseFileSystem getReleaseFileSystem() {
    return new ReleaseFileSystem(getDccFileSystem(), getRelease());
  }

  @Override
  public SubmissionReport getSubmissionReport() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasErrors() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getErrorCount() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void reportSummary(String fileName, String name, String value) {
    new UnsupportedOperationException();
  }

  @Override
  public void reportField(String fileName, FieldReport fieldReport) {
    new UnsupportedOperationException();
  }

  @Override
  public void reportError(String fileName, TupleError tupleError) {
    logError(fileName,
        tupleError.getLine(),
        tupleError.getColumnNames().toString(),
        tupleError.getValue(),
        tupleError.getType(),
        tupleError.getParameters().values().toArray());
  }

  @Override
  public void reportError(String fileName, long lineNumber, String columnName, Object value, ErrorType type,
      Object... params) {
    logError(fileName, lineNumber, columnName, value, type, params);
  }

  @Override
  public void reportError(String fileName, long lineNumber, Object value, ErrorType type, Object... params) {
    logError(fileName, lineNumber, null, value, type, params);
  }

  @Override
  public void reportError(String fileName, Object value, ErrorType type, Object... params) {
    logError(fileName, -1, null, value, type, params);
  }

  @Override
  public void reportError(String fileName, ErrorType type, Object... params) {
    logError(fileName, -1, null, null, type, params);
  }

  @Override
  public void reportError(String fileName, ErrorType type) {
    logError(fileName, -1, null, null, type, (Object[]) null);
  }

  @Override
  public void reportLineNumbers(Path path) {
    new UnsupportedOperationException();
  }

  private static void logError(String fileName, long lineNumber, String columnName, Object value, ErrorType type,
      Object... params) {
    val message =
        "[reportError] fileName = '%s', lineNumber = %s, columnName = %s, value = %s, type = %s, params = %s";
    val text = format(message, fileName, lineNumber, columnName, value, type, Arrays.toString(params));
    log.error("{}", text);
  }

  private static URL getDictionaryUrl(final java.lang.String version) throws MalformedURLException {
    val basePath = "http://seqwaremaven.oicr.on.ca/artifactory";
    val template = "%s/simple/dcc-dependencies/org/icgc/dcc/dcc-resources/%s/dcc-resources-%s.jar";
    URL url = new URL(format(template, basePath, version, version));

    return url;
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

  @Override
  public Optional<Path> getSsmPrimaryFile() {
    throw new UnsupportedOperationException();
  }

}