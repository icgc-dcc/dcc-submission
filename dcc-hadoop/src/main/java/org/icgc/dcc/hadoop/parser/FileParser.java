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
package org.icgc.dcc.hadoop.parser;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodecFactory;

import com.google.common.io.LineReader;

@RequiredArgsConstructor
public class FileParser<T> {

  private final FileSystem fileSystem;
  private final FileLineParser<T> lineParser;
  private final boolean processHeader;

  public long parse(Path filePath, FileRecordProcessor<T> recordProcessor) throws IOException {
    @Cleanup
    val inputStream = createInputStream(filePath);

    return parse(inputStream, recordProcessor);
  }

  public long parse(InputStream inputStream, FileRecordProcessor<T> recordProcessor) throws IOException {
    val reader = new LineReader(new InputStreamReader(inputStream));

    return parse(reader, recordProcessor);
  }

  public long parse(LineReader reader, FileRecordProcessor<T> recordProcessor) throws IOException {
    // Line state (one-based)
    long lineNumber = 1;
    String line;

    // Read all lines
    while ((line = reader.readLine()) != null) {
      val record = lineParser.parse(line);

      if (processHeader || lineNumber > 1) {
        // Delegate logic
        recordProcessor.process(lineNumber, record);
      }

      // Book-keeping
      lineNumber++;
    }

    return lineNumber - 1;
  }

  /**
   * TODO: move this to an FS abstraction.
   */
  private DataInputStream createInputStream(Path file) {
    val factory = new CompressionCodecFactory(fileSystem.getConf());

    try {
      val codec = factory.getCodec(file);
      InputStream inputStream =
          (codec == null) ? fileSystem.open(file) : codec.createInputStream(fileSystem.open(file));
      return new DataInputStream(inputStream);
    } catch (IOException e) {
      throw new RuntimeException("Error reading: '" + file.toString() + "'", e);
    }
  }

}
