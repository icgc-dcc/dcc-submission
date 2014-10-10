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
package org.icgc.dcc.submission.validation.primary.report;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.io.Files.getFileExtension;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableMap;

@Slf4j
public class ByteOffsetToLineNumberTest {

  /**
   * Test configuration.
   */
  private static final String TEST_DIR = "src/test/resources/fixtures/validation/line-numbers";

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  FileSystem fileSystem;

  @Before
  @SneakyThrows
  public void setUp() {
    fileSystem = FileSystem.getLocal(new Configuration());

    ByteOffsetToLineNumber.fileSystem = fileSystem;
  }

  @Test
  @SneakyThrows
  public void testConvert() {
    for (val file : new File(TEST_DIR).listFiles()) {
      log.info("Processing '{}'", file.getName());
      val expected = getMapping(file);
      val offsets = expected.keySet();
      log.info("Expected: {}", expected);

      Path path = new Path(tmp.newFile(file.getName()).getAbsolutePath());
      fileSystem.copyFromLocalFile(new Path(file.toURI()), path);

      // Exercise
      val actual = ByteOffsetToLineNumber.convert(path, offsets, false);

      assertThat(actual).isEqualTo(expected);
    }
  }

  @SneakyThrows
  private static Map<Long, Long> getMapping(File file) {
    val mapping = ImmutableMap.<Long, Long> builder();
    long offset = 0;
    long lineNumber = 1;

    for (val b : getBytes(file)) {
      offset++;
      if ((char) b == '\n') {
        lineNumber++;

        log.info("line {} = {} byte offset", lineNumber, offset);
        mapping.put(offset, lineNumber);
      }
    }

    return mapping.build();
  }

  @SneakyThrows
  private static byte[] getBytes(File file) {
    @Cleanup
    val input = new FileInputStream(file);
    val bytes = toByteArray(isGzip(file) ? new GZIPInputStream(input) : input);

    return bytes;
  }

  private static boolean isGzip(File file) {
    return getFileExtension(file.getName()).equals("gz");
  }

  @SneakyThrows
  private static FileSystem createFileSystem() {
    return FileSystem.getLocal(new Configuration());
  }

}
