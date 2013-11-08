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
import static org.fest.assertions.api.Assertions.assertThat;
import static org.icgc.dcc.submission.fs.hdfs.HadoopUtils.lsAll;

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
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

@Slf4j
public class ByteOffsetToLineNumberTest {

  /**
   * Test configuration.
   */
  private static final String TEST_DIR = "src/test/resources/fixtures/validation/line-numbers";
  private static final Path TEST_PATH = new Path(TEST_DIR);
  private static final FileSystem TEST_FILE_SYSTEM = createFileSystem();

  @Before
  @SneakyThrows
  public void setUp() {
    ByteOffsetToLineNumber.fileSystem = TEST_FILE_SYSTEM;
  }

  @Test
  @SneakyThrows
  public void testConvert() {
    val paths = lsAll(TEST_FILE_SYSTEM, TEST_PATH);
    for (val path : paths) {
      log.info("Processing '{}'", path.getName());
      val expected = getMapping(path);
      val offsets = expected.keySet();
      log.info("Expected: {}", expected);

      // Exercise
      val actual = ByteOffsetToLineNumber.convert(path, offsets);

      assertThat(actual).isEqualTo(expected);
    }
  }

  @SneakyThrows
  private static Map<Long, Long> getMapping(Path path) {
    val mapping = ImmutableMap.<Long, Long> builder();
    long offset = 0;
    long lineNumber = 1;

    for (val b : getBytes(path)) {
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
  private static byte[] getBytes(Path path) {
    @Cleanup
    val input = TEST_FILE_SYSTEM.open(path);
    val bytes = toByteArray(isGzip(path) ? new GZIPInputStream(input) : input);

    return bytes;
  }

  private static boolean isGzip(Path path) {
    return getFileExtension(path.getName()).equals("gz");
  }

  @SneakyThrows
  private static FileSystem createFileSystem() {
    return FileSystem.getLocal(new Configuration());
  }

}
