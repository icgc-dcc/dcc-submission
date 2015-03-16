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
package org.icgc.dcc.submission.validation.platform;

import static cascading.flow.FlowProps.setMaxConcurrentSteps;
import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.regex.Pattern.compile;
import static org.icgc.dcc.common.cascading.Fields2.fields;
import static org.icgc.dcc.common.core.util.Joiners.DOT;
import static org.icgc.dcc.common.core.util.Joiners.EXTENSION;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.lsFile;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.toFilenameList;
import static org.icgc.dcc.submission.validation.cascading.ValidationFields.OFFSET_FIELD;
import static org.icgc.dcc.submission.validation.primary.core.Plan.MAX_CONCURRENT_FLOW_STEPS;

import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.cascading.CascadingContext;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.common.core.util.Extensions;
import org.icgc.dcc.submission.validation.primary.core.FlowType;

import cascading.flow.FlowConnector;
import cascading.tap.Tap;
import cascading.tuple.Fields;

import com.google.common.io.LineReader;

@Slf4j
public abstract class BaseSubmissionPlatformStrategy implements SubmissionPlatformStrategy {

  protected final FileSystem fileSystem;
  private final Map<String, String> hadoopProperties;
  private final Path submissionDir;
  private final Path validationOutputDir;

  protected BaseSubmissionPlatformStrategy(
      @NonNull final Map<String, String> hadoopProperties,
      @NonNull final FileSystem fileSystem,
      @NonNull final Path input,
      @NonNull final Path output) {
    this.hadoopProperties = hadoopProperties;
    this.fileSystem = fileSystem;
    this.submissionDir = input;
    this.validationOutputDir = output;
  }

  protected abstract CascadingContext getCascadingContext();

  protected abstract Map<?, ?> augmentFlowProperties(final Map<?, ?> flowProperties);

  @Override
  public Tap<?, ?, ?> getReportTap(String fileName, FlowType type, String reportName) {
    val reportPath = getReportPath(fileName, type, reportName).toUri().getPath();
    log.info("Streaming through report: '{}'", reportPath);

    return getCascadingContext()
        .getTaps()
        .getCompressingJson(reportPath);
  }

  @Override
  public Tap<?, ?, ?> getSourceTap(String fileName) {
    return getCascadingContext()
        .getTaps()
        .getDecompressingLinesNoHeader(
            getFilePath(fileName),
            OFFSET_FIELD);
  }

  @Override
  public FlowConnector getFlowConnector() {
    Map<Object, Object> flowProperties = newHashMap();

    // From external application configuration file
    flowProperties.putAll(hadoopProperties);

    setMaxConcurrentSteps(flowProperties, MAX_CONCURRENT_FLOW_STEPS);

    return getCascadingContext()
        .getConnectors()
        .getFlowConnector(
            augmentFlowProperties(flowProperties));
  }

  /**
   * TODO: phase out in favour of {@link #getSourceTap(FileType)}; Temporary: see DCC-1876
   */
  @Override
  public Tap<?, ?, ?> getNormalizerSourceTap(String fileName) {
    return getCascadingContext()
        .getTaps()
        .getDecompressingTsvWithHeader(
            getFilePath(fileName));
  }

  protected Path getReportPath(String fileName, FlowType type, String reportName) {
    return new Path(
        validationOutputDir,
        EXTENSION.join(
            DOT.join(
                fileName,
                REPORT_FILES_INFO_JOINER.join(
                    type,
                    reportName)),
            Extensions.JSON));
  }

  @SneakyThrows
  public Fields getFileHeader(String fileName) {
    @Cleanup
    InputStreamReader isr = new InputStreamReader(
        fileSystem.open(getFile(fileName)),
        UTF_8);
    return fields(FIELD_SPLITTER.split(new LineReader(isr).readLine()));
  }

  @Override
  public List<String> listFileNames(String pattern) {
    return toFilenameList(lsFile(fileSystem, submissionDir, compile(pattern)));
  }

  public List<String> listFileNames() {
    return toFilenameList(lsFile(fileSystem, submissionDir));
  }

  @Override
  public Path getFile(String fileName) {
    return new Path(submissionDir, fileName);
  }

  protected String getFilePath(String fileName) {
    return getFile(fileName).toUri().toString();
  }

}
