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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.primary.core.FileSchemaDirectory;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.Key;

import cascading.flow.FlowConnector;
import cascading.scheme.hadoop.TextDelimited;
import cascading.scheme.hadoop.TextLine;
import cascading.tap.Tap;
import cascading.tuple.Fields;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

public interface PlatformStrategy {

  String FIELD_SEPARATOR = "\t";

  Splitter FIELD_SPLITTER = Splitter.on(FIELD_SEPARATOR);

  Joiner FIELD_JOINER = Joiner.on(FIELD_SEPARATOR);

  String FILE_NAME_SEPARATOR = "--";

  public FlowConnector getFlowConnector();

  /**
   * TODO: Adapt submission code to use {@link #getSourceTap2(FileSchema)} since we can now assume the header is known
   * (and therefore we should use {@link TextDelimited} rather than {@link TextLine}.
   */
  @Deprecated
  public Tap<?, ?, ?> getSourceTap(FileSchema schema);

  /**
   * See comment in {@link #getSourceTap(FileSchema)}.
   */
  public Tap<?, ?, ?> getSourceTap2(FileSchema schema);

  public Tap<?, ?, ?> getFlowSinkTap(FileSchema schema, FlowType type);

  public Tap<?, ?, ?> getTrimmedTap(Key key);

  public Tap<?, ?, ?> getReportTap(FileSchema schema, FlowType type, String reportName);

  /**
   * Used to read back a report that was produced during the execution of a Flow. This does not use a Tap so that it can
   * be executed outside of a Flow.
   * @throws IOException
   */
  public InputStream readReportTap(FileSchema schema, FlowType type, String reportName) throws FileNotFoundException,
      IOException;

  /**
   * Necessary until DCC-996 is done (IF there is indeed a more elegant alternative).
   */
  public Fields getFileHeader(FileSchema schema) throws IOException;

  public Path path(final FileSchema schema) throws FileNotFoundException, IOException;

  public FileSchemaDirectory getFileSchemaDirectory();

  public FileSchemaDirectory getSystemDirectory();
}
