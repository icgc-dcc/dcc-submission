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

import static org.icgc.dcc.submission.validation.core.Validators.checkInterrupted;
import static org.icgc.dcc.submission.validation.key.report.KVReport.REPORT_FILE_NAME;

import java.io.IOException;
import java.io.InputStream;

import lombok.Cleanup;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.submission.validation.cascading.CascadeExecutor;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.key.core.KVValidatorRunner;
import org.icgc.dcc.submission.validation.key.error.KVError;

@NoArgsConstructor
@Slf4j
public class KeyValidator implements Validator {

  /**
   * The name of the component.
   */
  public static final String COMPONENT_NAME = "Key Validator";

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public void validate(ValidationContext context) throws InterruptedException {
    val reportPath = getReportPath(context);
    val runner = createRunner(context, reportPath);

    log.info("Starting key validation...");
    execute(context, runner);
    log.info("Finished key validation");

    checkInterrupted(getName());

    log.info("Starting key validation report collection...");
    collect(context, reportPath);
    log.info("Finished key validation report collection");
  }

  private static KVValidatorRunner createRunner(ValidationContext context, Path reportPath) {
    return new KVValidatorRunner(
        getOldReleasePath(context).toUri().toString(),
        getNewReleasePath(context).toUri().toString(),
        reportPath.toUri().toString());
  }

  private static Path getReportPath(ValidationContext context) {
    val validationDir = context.getSubmissionDirectory().getValidationDirPath();

    return new Path(validationDir, REPORT_FILE_NAME);
  }

  private static Path getOldReleasePath(ValidationContext context) {
    return new Path(context.getPreviousSubmissionDirectory().getSubmissionDirPath());
  }

  private static Path getNewReleasePath(ValidationContext context) {
    return new Path(context.getSubmissionDirectory().getSubmissionDirPath());
  }

  private static void execute(ValidationContext context, KVValidatorRunner runnable) {
    val executor = new CascadeExecutor(context.getPlatformStrategy());

    executor.execute(runnable);
  }

  @SneakyThrows
  private static void collect(ValidationContext context, Path reportPath) {
    @Cleanup
    val inputStream = createInputStream(context.getFileSystem(), reportPath);
    val errors = getErrors(inputStream);

    while (errors.hasNext()) {
      val error = errors.next();

      context.reportError(error.getFileName(), error.getType());
    }
  }

  private static InputStream createInputStream(FileSystem fileSystem, Path file) {
    val factory = new CompressionCodecFactory(fileSystem.getConf());

    try {
      val codec = factory.getCodec(file);
      val baseInputStream = fileSystem.open(file);
      return codec == null ? baseInputStream : codec.createInputStream(fileSystem.open(file));
    } catch (IOException e) {
      throw new RuntimeException("Error reading: '" + file.toString() + "'", e);
    }
  }

  @SneakyThrows
  private static MappingIterator<KVError> getErrors(InputStream inputStream) {
    val reader = new ObjectMapper().reader().withType(KVError.class);

    return reader.readValues(inputStream);
  }

}
