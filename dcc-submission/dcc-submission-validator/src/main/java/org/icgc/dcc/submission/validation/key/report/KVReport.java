/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.key.report;

import static org.codehaus.jackson.JsonGenerator.Feature.AUTO_CLOSE_TARGET;
import static org.codehaus.jackson.map.SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import lombok.NonNull;
import lombok.SneakyThrows;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.icgc.dcc.submission.validation.key.error.KVReportError;

public class KVReport implements Closeable {

  /**
   * The file name of the produced key validation report.
   */
  public static final String REPORT_FILE_NAME = "all.keys--errors.json";

  private final static ObjectWriter WRITER = new ObjectMapper(new JsonFactory().disable(AUTO_CLOSE_TARGET))
      .configure(FAIL_ON_EMPTY_BEANS, false)
      .writer();

  @NonNull
  private final FileSystem fileSystem;
  @NonNull
  private final Path path;
  @NonNull
  private final OutputStream outputStream;

  @SneakyThrows
  public KVReport(FileSystem fileSystem, Path path) {
    this.fileSystem = fileSystem;
    this.path = path;
    this.outputStream = fileSystem.create(path);
  }

  @SneakyThrows
  public void report(KVReportError error) {
    WRITER.writeValue(outputStream, error);
  }

  @Override
  public void close() throws IOException {
    outputStream.close();
  }

}
