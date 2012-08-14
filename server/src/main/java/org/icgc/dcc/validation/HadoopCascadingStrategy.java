/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.validation.cascading.HadoopJsonScheme;
import org.icgc.dcc.validation.cascading.ValidationFields;

import cascading.flow.FlowConnector;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.scheme.hadoop.TextDelimited;
import cascading.scheme.hadoop.TextLine;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tuple.Fields;

public class HadoopCascadingStrategy extends BaseCascadingStrategy {

  private final HadoopFileSchemaDirectory fileSchemaDirectory;

  public HadoopCascadingStrategy(FileSystem fileSystem, Path source, Path output) {
    super(fileSystem, source, output);
    this.fileSchemaDirectory = new HadoopFileSchemaDirectory(fileSystem, source);
  }

  @Override
  public FlowConnector getFlowConnector() {
    return new HadoopFlowConnector();
  }

  @Override
  public Tap<?, ?, ?> getReportTap(FileSchema schema, FlowType type, String reportName) {
    Path path = reportPath(schema, type, reportName);
    return new Hfs(new HadoopJsonScheme(), path.toUri().getPath());
  }

  @Override
  protected Tap<?, ?, ?> tap(Path path) {
    return new Hfs(new TextDelimited(true, "\t"), path.toUri().getPath());
  }

  @Override
  protected Tap<?, ?, ?> tap(Path path, Fields fields) {
    return new Hfs(new TextDelimited(fields, true, "\t"), path.toUri().getPath());
  }

  @Override
  protected Tap<?, ?, ?> tapSource(Path path) {
    return new Hfs(new TextLine(new Fields(ValidationFields.OFFSET_FIELD_NAME, "line")), path.toUri().getPath());
  }

  @Override
  public FileSchemaDirectory getFileSchemaDirectory() {
    return this.fileSchemaDirectory;
  }

  private static class HadoopFileSchemaDirectory implements FileSchemaDirectory {

    private final Path directory;

    private final FileSystem fs;

    public HadoopFileSchemaDirectory(FileSystem fs, Path source) {
      checkArgument(source != null);
      checkArgument(fs != null);
      this.directory = source;
      this.fs = fs;
    }

    @Override
    public String getFile(FileSchema fileSchema) {
      List<Path> paths = matches(fileSchema);
      if(paths == null || paths.size() == 0) {
        throw new IllegalArgumentException();
      }
      if(paths.size() > 1) {
        throw new IllegalStateException();
      }
      return paths.get(0).toString();
    }

    @Override
    public boolean hasFile(final FileSchema fileSchema) {
      List<Path> paths = matches(fileSchema);
      return paths != null && paths.size() > 0;
    }

    private List<Path> matches(final FileSchema fileSchema) {
      if(fileSchema.getPattern() == null) {
        return null;
      }
      return HadoopUtils.lsFile(fs, directory.toString(), Pattern.compile(fileSchema.getPattern()));
    }
  }
}
