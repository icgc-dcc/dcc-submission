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
package org.icgc.dcc.submission.core.util;

import static com.google.common.io.Files.getFileExtension;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.BZip2Codec;

import com.google.common.io.LineReader;

@RequiredArgsConstructor
public class FileParser<T> {

  private final FileSystem fileSystem;
  private final FileLineParser<T> lineParser;

  public void parse(Path filePath, FileRecordProcessor<T> recordProcessor) throws IOException {
    @Cleanup
    val inputStream = getInputStream(filePath, fileSystem);

    parse(inputStream, recordProcessor);
  }

  public void parse(InputStream inputStream, FileRecordProcessor<T> recordProcessor) throws IOException {
    val reader = new LineReader(new InputStreamReader(inputStream));

    parse(reader, recordProcessor);
  }

  public void parse(LineReader reader, FileRecordProcessor<T> recordProcessor) throws IOException {
    // Line state (one-based)
    long lineNumber = 1;
    String line;

    // Read all lines
    while ((line = reader.readLine()) != null) {
      val record = lineParser.parse(line);

      if (lineNumber > 1) {
        // Delegate logic
        recordProcessor.process(lineNumber, record);
      }

      // Book-keeping
      lineNumber++;
    }
  }

  /**
   * Returns an {@code InputStream} capable of reading {@code gz}, {@code bzip2} or plain text files.
   */
  private static InputStream getInputStream(Path path, FileSystem fileSystem) throws IOException {
    val extension = getFileExtension(path.getName());
    val gzip = extension.equals("gz");
    val bzip2 = extension.equals("bz2");

    // @formatter:off
    val inputStream = fileSystem.open(path);
    return 
        gzip  ? new GZIPInputStream(inputStream)                : 
        bzip2 ? new BZip2Codec().createInputStream(inputStream) :
                inputStream;
    // @formatter:on
  }

}
