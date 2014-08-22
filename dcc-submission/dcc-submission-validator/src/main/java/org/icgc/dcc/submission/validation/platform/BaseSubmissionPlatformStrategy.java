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
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static org.icgc.dcc.hadoop.cascading.Fields2.fields;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.toFilenameList;
import static org.icgc.dcc.submission.validation.primary.core.Plan.MAX_CONCURRENT_FLOW_STEPS;

import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.hadoop.cascading.connector.CascadingConnectors;
import org.icgc.dcc.hadoop.cascading.taps.CascadingTaps;
import org.icgc.dcc.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.validation.primary.core.FlowType;

import cascading.flow.FlowConnector;
import cascading.tap.Tap;
import cascading.tuple.Fields;

import com.google.common.io.LineReader;

public abstract class BaseSubmissionPlatformStrategy implements SubmissionPlatformStrategy {

  private final CascadingTaps taps;
  private final CascadingConnectors connectors;

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
    this.taps = getTaps();
    this.connectors = getConnectors();
  }

  protected abstract CascadingTaps getTaps();

  protected abstract CascadingConnectors getConnectors();

  @Override
  public FlowConnector getFlowConnector() {
    Map<Object, Object> flowProperties = newHashMap();

    // From external application configuration file
    flowProperties.putAll(hadoopProperties);

    setMaxConcurrentSteps(flowProperties, MAX_CONCURRENT_FLOW_STEPS);

    return connectors.getFlowConnector(augmentFlowProperties(flowProperties));
  }

  protected abstract Map<?, ?> augmentFlowProperties(final Map<?, ?> flowProperties);

  /**
   * TODO: phase out in favour of {@link #getSourceTap(FileType)}; Temporary: see DCC-1876
   */
  @Override
  public Tap<?, ?, ?> getNormalizerSourceTap(String fileName) {
    return taps.getDecompressingTsvWithHeader(getFilePath(fileName));
  }

  protected Path getReportPath(String fileName, FlowType type, String reportName) {
    return new Path(
        validationOutputDir,
        format("%s.%s%s%s.json",
            fileName,
            type,
            FILE_NAME_SEPARATOR,
            reportName));
  }

  /**
   * Returns a tap for the given path.
   */
  protected abstract Tap<?, ?, ?> tap(Path path);

  protected abstract Tap<?, ?, ?> tap(Path path, Fields fields);

  @Override
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
    return toFilenameList(HadoopUtils.lsFile(fileSystem, submissionDir, compile(pattern)));
  }

  @Override
  public List<String> listFileNames() {
    return toFilenameList(HadoopUtils.lsFile(fileSystem, submissionDir));
  }

  @Override
  public Path getFile(String fileName) {
    return new Path(submissionDir, fileName);
  }

  protected String getFilePath(String fileName) {
    return getFile(fileName).toUri().toString();
  }

}
