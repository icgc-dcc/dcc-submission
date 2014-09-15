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
package org.icgc.dcc.submission.validation.rgv.report;

import static com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET;
import static com.google.common.io.Files.getNameWithoutExtension;
import static java.lang.String.format;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import lombok.val;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.validation.cascading.TupleState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class TupleStateWriter implements Closeable {

  private static final ObjectWriter OBJECT_WRITER = new ObjectMapper()
      .configure(AUTO_CLOSE_TARGET, false)
      .writer()
      .withDefaultPrettyPrinter();

  private final OutputStream outputStream;

  public TupleStateWriter(FileSystem fileSystem, Path outputDirectory, Path file) throws IOException {
    this.outputStream = getOutputStream(fileSystem, outputDirectory, file);
  }

  public void write(TupleState tupleState) throws IOException {
    OBJECT_WRITER.writeValue(outputStream, tupleState);
  }

  @Override
  public void close() throws IOException {
    outputStream.close();
  }

  /**
   * Returns a {@code OutputStream} to capture all reported errors.
   */
  private static OutputStream getOutputStream(FileSystem fileSystem, Path outputDirectory, Path file)
      throws IOException {
    val fileName = format("%s.rgv--errors.json", getNameWithoutExtension(file.getName()));
    val reportPath = new Path(outputDirectory, fileName);

    return fileSystem.create(reportPath);
  }

}
