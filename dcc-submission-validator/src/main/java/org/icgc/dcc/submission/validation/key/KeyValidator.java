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
package org.icgc.dcc.submission.validation.key;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;
import static org.apache.hadoop.mapred.JobConf.MAPRED_MAP_TASK_JAVA_OPTS;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.checkExistence;
import static org.icgc.dcc.common.hadoop.util.HadoopConstants.MR_JOBTRACKER_ADDRESS_KEY;
import static org.icgc.dcc.submission.validation.core.Validators.checkInterrupted;
import static org.icgc.dcc.submission.validation.key.report.KVReporter.REPORT_FILE_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import lombok.Cleanup;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.icgc.dcc.common.cascading.FlowExecutor;
import org.icgc.dcc.submission.core.report.Error;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.key.core.KVValidatorRunner;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

@NoArgsConstructor
@Slf4j
public class KeyValidator implements Validator {

  /**
   * The name of the component.
   */
  public static final String COMPONENT_NAME = "Key Validator";

  /**
   * The size of the heap used when running in non-local mode.
   */
  private static final String DEFAULT_MAX_HEAP_SIZE = "24g";

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
    val submissionDirectory = context.getSubmissionDirectory();
    return new KVValidatorRunner(
        context.getFileSystem().getUri(),
        context.getDataTypes(),
        context.getDictionary(),
        submissionDirectory.getSubmissionDirPath(),
        submissionDirectory.getSystemDirPath(),
        reportPath.toUri().toString());
  }

  private static Path getReportPath(ValidationContext context) {
    val validationDir = context.getSubmissionDirectory().getValidationDirPath();

    return new Path(validationDir, REPORT_FILE_NAME);
  }

  @SneakyThrows
  private static void execute(ValidationContext context, KVValidatorRunner runner) {
    // Change this switch to false to aid in step debugging
    val distributable = true;
    if (distributable) {
      // Run on cluster if using HDFS
      val properties = getProperties(context);
      val executor = new FlowExecutor(properties);

      executor.execute(runner);
    } else {
      // Run on this node
      runner.execute(new Configuration());
    }
  }

  private static Map<Object, Object> getProperties(ValidationContext context) {
    // Needed for the core hadoop properties
    val hadoop = context.getPlatformStrategy().getFlowConnector().getProperties();

    // This can't be an immutable map since the values can be null
    val properties = newHashMap();
    properties.put(MAPRED_MAP_TASK_JAVA_OPTS, "-Xmx" + DEFAULT_MAX_HEAP_SIZE);
    properties.put(FS_DEFAULT_NAME_KEY, hadoop.get(FS_DEFAULT_NAME_KEY));
    properties.put(MR_JOBTRACKER_ADDRESS_KEY, hadoop.get(MR_JOBTRACKER_ADDRESS_KEY));

    return properties;
  }

  @SneakyThrows
  private static void collect(ValidationContext context, Path reportPath) {
    if (!checkExistence(context.getFileSystem(), reportPath)) {
      log.info("Report file '{}' does not exist. Skipping report collection", reportPath);
      return;
    }

    @Cleanup
    val inputStream = createInputStream(context.getFileSystem(), reportPath);
    val errors = getErrors(inputStream);

    while (errors.hasNext()) {
      val error = errors.next();
      val fileName = error.getFileName();
      val fileType = context.getDictionary().getFileType(fileName);
      checkState(fileType.isPresent(),
          "Expecting a corresponding file type for file name '{}'", fileName);

      context.reportError(error);
    }
  }

  private static InputStream createInputStream(FileSystem fileSystem, Path path) {
    val factory = new CompressionCodecFactory(fileSystem.getConf());

    try {
      val codec = factory.getCodec(path);
      val baseInputStream = fileSystem.open(path);
      return codec == null ? baseInputStream : codec.createInputStream(fileSystem.open(path));
    } catch (IOException e) {
      throw new RuntimeException("Error reading: '" + path.toString() + "'", e);
    }
  }

  @SneakyThrows
  private static MappingIterator<Error> getErrors(
      InputStream inputStream) {
    val reader = new ObjectMapper().reader().withType(Error.class);

    return reader.readValues(inputStream);
  }
}
