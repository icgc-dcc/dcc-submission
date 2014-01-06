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
package org.icgc.dcc.submission.validation.key;

<<<<<<< HEAD
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;
import org.icgc.dcc.submission.validation.core.ErrorType;
import org.icgc.dcc.submission.validation.core.FieldReport;
import org.icgc.dcc.submission.validation.core.SubmissionReport;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.platform.LocalPlatformStrategy;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;

import com.google.common.base.Optional;
=======
import lombok.val;
>>>>>>> 5c9a53bc83b9ac49b4618a9f05988b41f72e3af8

/**
 * Command-line utility to initiate key validation on a specified project stored locally or in HDFS. Will use Cascading
 * local or Hadoop depending on the {@code fsUrl} argument's scheme.
 */
public class Main {

  public static void main(String... args) throws InterruptedException {
    // Resolve configuration
    int i = 0;
    val previousReleaseName = args.length >= ++i ? args[i - 1] : "release1";
    val releaseName = args.length >= ++i ? args[i - 1] : "release2";
    val projectKey = args.length >= ++i ? args[i - 1] : "project1";
    val fsRoot = args.length >= ++i ? args[i - 1] : "/tmp/dcc_root_dir";
    val fsUrl = args.length >= ++i ? args[i - 1] : "file:///";
    val context = new KeyValidationContext(previousReleaseName, releaseName, projectKey, fsRoot, fsUrl);

    // Validate
    validate(context);
  }

<<<<<<< HEAD
  @RequiredArgsConstructor
  @Slf4j
  private static class KeyValidationContext implements ValidationContext {

    private final String projectKey;
    private final String releaseName;

    private final String inputDir;
    private final String outputDir;
    private final String systemDir;

    @Override
    public PlatformStrategy getPlatformStrategy() {
      // Round about way to get the inputs and outputs
      Path input = new Path(inputDir);
      log.info("Validation context for '{}' has inputDir = {}", projectKey, inputDir);
      Path output = new Path(outputDir);
      log.info("Validation context for '{}' has outputDir = {}", projectKey, outputDir);
      Path system = new Path(systemDir);
      log.info("Validation context for '{}' has systemDir = {}", projectKey, systemDir);

      return new LocalPlatformStrategy(input, output, system);
    }

    @Override
    public String getProjectKey() {
      return null;
    }

    @Override
    public List<String> getEmails() {
      return null;
    }

    @Override
    public Release getRelease() {
      return new Release(releaseName);
    }

    @Override
    public Dictionary getDictionary() {
      return null;
    }

    @Override
    public SubmissionDirectory getSubmissionDirectory() {
      return null;
    }

    @Override
    public Optional<Path> getSsmPrimaryFile() {
      return null;
    }

    @Override
    public FileSchema getSsmPrimaryFileSchema() {
      return null;
    }

    @Override
    public DccFileSystem getDccFileSystem() {
      return null;
    }

    @Override
    @SneakyThrows
    public FileSystem getFileSystem() {
      return FileSystem.getLocal(new Configuration());
    }

    @Override
    public ReleaseFileSystem getReleaseFileSystem() {
      return null;
    }

    @Override
    public SubmissionReport getSubmissionReport() {
      return null;
    }

    @Override
    public boolean hasErrors() {
      return false;
    }

    @Override
    public int getErrorCount() {
      return 0;
    }

    @Override
    public void reportSummary(String fileName, String name, String value) {
    }

    @Override
    public void reportField(String fileName, FieldReport fieldReport) {
    }

    @Override
    public void reportError(String fileName, TupleError tupleError) {
    }

    @Override
    public void reportError(String fileName, long lineNumber, String columnName, Object value, ErrorType type,
        Object... params) {
    }

    @Override
    public void reportError(String fileName, long lineNumber, Object value, ErrorType type, Object... params) {
    }

    @Override
    public void reportError(String fileName, Object value, ErrorType type, Object... params) {
    }

    @Override
    public void reportError(String fileName, ErrorType type, Object... params) {
    }

    @Override
    public void reportError(String fileName, ErrorType type) {
      System.out.println("reportError: " + fileName + " - " + type);
    }

    @Override
    public void reportLineNumbers(Path path) {
    }

=======
  private static void validate(KeyValidationContext context) throws InterruptedException {
    val validator = new KeyValidator();

    validator.validate(context);
>>>>>>> 5c9a53bc83b9ac49b4618a9f05988b41f72e3af8
  }

}
