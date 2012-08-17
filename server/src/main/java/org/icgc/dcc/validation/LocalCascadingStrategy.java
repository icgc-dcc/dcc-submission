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

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.validation.cascading.LocalJsonScheme;
import org.icgc.dcc.validation.cascading.ValidationFields;

import cascading.flow.FlowConnector;
import cascading.flow.local.LocalFlowConnector;
import cascading.scheme.local.TextDelimited;
import cascading.scheme.local.TextLine;
import cascading.tap.Tap;
import cascading.tap.local.FileTap;
import cascading.tuple.Fields;

/**
 * 
 */
public class LocalCascadingStrategy extends BaseCascadingStrategy {

  public LocalCascadingStrategy(Path source, Path output, Path system) {
    super(localFileSystem(), source, output, system);
  }

  @Override
  public FlowConnector getFlowConnector() {
    return new LocalFlowConnector();
  }

  @Override
  public Tap<?, ?, ?> getReportTap(FileSchema schema, FlowType type, String reportName) {
    return new FileTap(new LocalJsonScheme(), reportPath(schema, type, reportName).toUri().getPath());
  }

  @Override
  protected Tap<?, ?, ?> tap(Path path) {
    return new FileTap(new TextDelimited(true, "\t"), path.toUri().getPath());
  }

  @Override
  protected Tap<?, ?, ?> tap(Path path, Fields fields) {
    return new FileTap(new TextDelimited(fields, true, "\t"), path.toUri().getPath());
  }

  @Override
  protected Tap<?, ?, ?> tapSource(Path path) {
    return new FileTap(new TextLine(new Fields(ValidationFields.OFFSET_FIELD_NAME, "line")), path.toUri().getPath());
  }

  static FileSystem localFileSystem() {
    try {
      return FileSystem.getLocal(new Configuration());
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }
}
