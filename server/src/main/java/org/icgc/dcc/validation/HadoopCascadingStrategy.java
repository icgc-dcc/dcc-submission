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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.icgc.dcc.dictionary.model.FileSchema;

import cascading.flow.FlowConnector;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.scheme.hadoop.TextDelimited;
import cascading.scheme.hadoop.TextLine;
import cascading.tap.Tap;
import cascading.tap.hadoop.Lfs;
import cascading.tuple.Fields;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;

/**
 * 
 */
public class HadoopCascadingStrategy implements CascadingStrategy {

  private final File source;

  private final File output;

  public HadoopCascadingStrategy(File source, File output) {
    this.source = source;
    this.output = output;
  }

  @Override
  public FlowConnector getFlowConnector() {
    return new HadoopFlowConnector();
  }

  @Override
  public Tap<?, ?, ?> getSourceTap(FileSchema schema) {
    return tapSource(file(schema));
  }

  @Override
  public Tap<?, ?, ?> getInternalSinkTap(FileSchema schema) {
    return tap(new File(output, schema.getName() + ".internal.tsv"));
  }

  @Override
  public Tap<?, ?, ?> getExternalSinkTap(FileSchema schema) {
    return tap(new File(output, schema.getName() + ".external.tsv"));
  }

  @Override
  public Tap<?, ?, ?> getTrimmedTap(Trim trim) {
    File trimmed = new File(output, trim.getSchema() + "#" + Joiner.on("-").join(trim.getFields()) + ".tsv");
    return new Lfs(new TextDelimited(new Fields(trim.getFields()), true, "\t"), trimmed.getAbsolutePath());
  }

  private Tap<?, ?, ?> tap(File file) {
    return new Lfs(new TextDelimited(true, "\t"), file.getAbsolutePath());
  }

  private Tap<?, ?, ?> tapSource(File file) {
    return new Lfs(new TextLine(new Fields("offset", "line")), file.getAbsolutePath());
  }

  private File file(final FileSchema schema) {
    File[] files = source.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.getName().contains(schema.getName());
        // return Pattern.matches(fs.getPattern(), pathname.getName());
      }
    });
    return files[0];
  }

  @Override
  public Fields getFileHeader(FileSchema schema) throws IOException {

    String firstLine = Files.readFirstLine(file(schema), Charsets.UTF_8);

    Iterable<String> header = Splitter.on('\t').split(firstLine);

    return new Fields(Iterables.toArray(header, String.class));
  }
}
