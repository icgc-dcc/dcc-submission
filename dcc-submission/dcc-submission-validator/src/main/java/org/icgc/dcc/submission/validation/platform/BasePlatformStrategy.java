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

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static org.icgc.dcc.hadoop.cascading.Fields2.fields;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.toFilenameList;

import java.io.InputStreamReader;
import java.util.List;

import lombok.Cleanup;
import lombok.SneakyThrows;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.FileSchemaRole;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.Key;

import cascading.flow.FlowConnector;
import cascading.tap.Tap;
import cascading.tuple.Fields;

import com.google.common.collect.Maps;
import com.google.common.io.LineReader;

public abstract class BasePlatformStrategy implements PlatformStrategy {

  protected final FileSystem fileSystem;
  private final Path submissionDir;
  private final Path validationOutputDir;

  /**
   * TODO: still needed?
   */
  private final Path system;

  protected BasePlatformStrategy(FileSystem fileSystem, Path input, Path output, Path system) {
    this.fileSystem = fileSystem;
    this.submissionDir = input;
    this.validationOutputDir = output;
    this.system = system;
  }

  @Override
  public FlowConnector getFlowConnector() {
    return getFlowConnector(Maps.<String, String> newLinkedHashMap());
  }

  /**
   * TODO: phase out in favour of {@link #getSourceTap(FileType)}; Temporary: see DCC-1876
   */
  @Override
  public Tap<?, ?, ?> getSourceTap2(String fileName) {
    return tapSource2(getFilePath(fileName));
  }

  @Override
  public Tap<?, ?, ?> getTrimmedTap(Key key) {
    return tap(trimmedPath(key), new Fields(key.getFields()));
  }

  protected Path trimmedPath(Key key) {
    checkState(false, "Should not be used"); // Not maintained anymore and due for deletion
    if (key.getRole() == FileSchemaRole.SUBMISSION) {
      return new Path(validationOutputDir, key.getName() + ".tsv");
    } else if (key.getRole() == FileSchemaRole.SYSTEM) {
      return new Path(new Path(system, DccFileSystem.VALIDATION_DIRNAME), key.getName() + ".tsv"); // TODO: should use
                                                                                                   // DccFileSystem
                                                                                                   // abstraction
    } else {
      throw new RuntimeException("Undefined File Schema Role " + key.getRole());
    }
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

  /**
   * See {@link #getSourceTap(FileSchema)} comment
   */
  protected abstract Tap<?, ?, ?> tapSource2(Path path);

  @Override
  @SneakyThrows
  public Fields getFileHeader(String fileName) {
    @Cleanup
    InputStreamReader isr = new InputStreamReader(
        fileSystem.open(getFilePath(fileName)),
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
  public Path getFilePath(String fileName) {
    return new Path(submissionDir, fileName);
  }

}
