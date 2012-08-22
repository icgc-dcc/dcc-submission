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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.FileSchemaRole;
import org.icgc.dcc.filesystem.DccFileSystem;

import cascading.tap.Tap;
import cascading.tuple.Fields;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.io.LineReader;

public abstract class BaseCascadingStrategy implements CascadingStrategy {

  private final FileSystem fileSystem;

  private final Path input;

  private final Path output;

  private final Path system;

  private final FileSchemaDirectory fileSchemaDirectory;

  private final FileSchemaDirectory systemDirectory;

  protected BaseCascadingStrategy(FileSystem fileSystem, Path input, Path output, Path system) {
    this.fileSystem = fileSystem;
    this.input = input;
    this.output = output;
    this.system = system;
    this.fileSchemaDirectory = new FileSchemaDirectory(fileSystem, input);
    this.systemDirectory = new FileSchemaDirectory(fileSystem, system);
  }

  @Override
  public Tap<?, ?, ?> getSourceTap(FileSchema schema) {
    try {
      if(schema.getRole() == FileSchemaRole.SUBMISSION) {
        return tapSource(path(schema));
      } else if(schema.getRole() == FileSchemaRole.SYSTEM) {
        return tapSource(systemPath(schema));
      } else {
        throw new RuntimeException("undefined File Schema Role " + schema.getRole());
      }
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Tap<?, ?, ?> getFlowSinkTap(FileSchema schema, FlowType flowType) {
    return tap(new Path(output, String.format("%s.%s.tsv", schema.getName(), flowType)));
  }

  @Override
  public Tap<?, ?, ?> getTrimmedTap(Trim trim) {
    return tap(trimmedPath(trim), new Fields(trim.getFields()));
  }

  @Override
  public InputStream readReportTap(FileSchema schema, FlowType type, String reportName) throws IOException {
    Path reportPath = reportPath(schema, type, reportName);
    return fileSystem.open(reportPath);
  }

  @Override
  public Fields getFileHeader(FileSchema schema) throws IOException {
    Path path;
    if(schema.getRole() == FileSchemaRole.SUBMISSION) {
      path = this.path(schema);
    } else if(schema.getRole() == FileSchemaRole.SYSTEM) {
      path = this.systemPath(schema);
    } else {
      throw new RuntimeException("File Schema role is not defined");
    }
    InputStreamReader isr = null;
    try {
      isr = new InputStreamReader(fileSystem.open(path), Charsets.UTF_8);
      LineReader lineReader = new LineReader(isr);
      String firstLine = lineReader.readLine();
      Iterable<String> header = Splitter.on('\t').split(firstLine);
      String dupHeader = this.checkDuplicateHeader(header);
      if(dupHeader != null) {
        throw new DuplicateHeaderException(dupHeader);
      }
      return new Fields(Iterables.toArray(header, String.class));
    } finally {
      Closeables.closeQuietly(isr);
    }
  }

  protected Path trimmedPath(Trim trim) {
    if(trim.getSchema().getRole() == FileSchemaRole.SUBMISSION) {
      return new Path(output, trim.getPath() + ".tsv");
    } else if(trim.getSchema().getRole() == FileSchemaRole.SYSTEM) {
      return new Path(new Path(system, DccFileSystem.VALIDATION_DIRNAME), trim.getPath() + ".tsv");
    } else {
      throw new RuntimeException("undefined File Schema Role " + trim.getSchema().getRole());
    }
  }

  protected Path reportPath(FileSchema schema, FlowType type, String reportName) {
    return new Path(output, String.format("%s.%s#%s.json", schema.getName(), type, reportName));
  }

  protected abstract Tap<?, ?, ?> tap(Path path);

  protected abstract Tap<?, ?, ?> tap(Path path, Fields fields);

  protected abstract Tap<?, ?, ?> tapSource(Path path);

  @Override
  public Path path(final FileSchema schema) throws FileNotFoundException, IOException {
    RemoteIterator<LocatedFileStatus> files = fileSystem.listFiles(input, false);
    while(files.hasNext()) {
      LocatedFileStatus file = files.next();
      if(file.isFile()) {
        Path path = file.getPath();
        if(Pattern.matches(schema.getPattern(), path.getName())) {
          return path;
        }
      }
    }
    throw new FileNotFoundException("no file for schema " + schema.getName());
  }

  private Path systemPath(final FileSchema schema) throws FileNotFoundException, IOException {

    RemoteIterator<LocatedFileStatus> files = fileSystem.listFiles(system, false);
    while(files.hasNext()) {
      LocatedFileStatus file = files.next();
      if(file.isFile()) {
        Path path = file.getPath();
        if(Pattern.matches(schema.getPattern(), path.getName())) {
          return path;
        }
      }
    }
    throw new FileNotFoundException("no file for schema " + schema.getName());

  }

  @Override
  public FileSchemaDirectory getFileSchemaDirectory() {
    return this.fileSchemaDirectory;
  }

  @Override
  public FileSchemaDirectory getSystemDirectory() {
    return this.systemDirectory;
  }

  private String checkDuplicateHeader(Iterable<String> header) {
    String[] headers = Iterables.toArray(header, String.class);
    Set<String> headerSet = Sets.newHashSet();

    for(int i = 0; i < headers.length; i++) {
      if(headerSet.contains(headers[i])) {
        return headers[i];
      } else {
        headerSet.add(headers[i]);
      }
    }
    return null;
  }
}
