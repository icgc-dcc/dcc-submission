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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.sort;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodecFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

// TODO: Make non-static class
@Slf4j
public class ByteOffsetToLineNumber {

  private static final int BUFFER_SIZE_BYTES = 1000 * 1000;

  @Inject
  protected static FileSystem fileSystem;

  public static Map<Long, Long> convert(@NonNull Path file, @NonNull Collection<Long> offsets) {
    return convert(file, offsets, true);
  }

  public static Map<Long, Long> convert(@NonNull Path file, @NonNull Collection<Long> offsets, boolean check) {
    if (check && !isHdfs()) {
      log.info("Local filesystem: not remapping line numbers for path: '{}'", file);
      return null;
    }

    log.info("Hdfs: remapping line numbers for path: '{}'", file);
    checkNotNull(fileSystem);

    // Need to sort offsets to ensure correct iteration order
    val sortedOffsets = sortOffsets(offsets);
    log.info("Offsets: {}", sortedOffsets);

    val mapping = buildByteToLineOffsetMap(file, sortedOffsets);

    return mapping;
  }

  @SneakyThrows
  private static Map<Long, Long> buildByteToLineOffsetMap(Path file, List<Long> sortedOffsets) {
    @Cleanup
    val inputStream = createInputStream(file);

    val mapping = Maps.<Long, Long> newLinkedHashMap();
    long previousOffset = 0;
    long lineOffset = 1; // 1-based

    for (Long byteOffset : sortedOffsets) {
      long currentOffset = byteOffset.longValue();
      if (currentOffset == -1L) {
        // File level error maybe? Dunno...
        mapping.put(-1L, -1L);
      } else {
        checkState(currentOffset >= 0, "Current offset is negative: %s", currentOffset);

        // No two same offsets
        checkState(currentOffset > previousOffset,
            "Current offset %s is greater than previous offset %s", currentOffset, previousOffset);

        lineOffset += countLinesInInterval(inputStream, previousOffset, currentOffset);
        mapping.put(byteOffset, lineOffset);

        previousOffset = byteOffset;
      }
    }

    return mapping;
  }

  private static long countLinesInInterval(DataInputStream is, long previousOffset, long currentOffset) {
    long difference = currentOffset - previousOffset;
    long quotient = (long) Math.floor(difference / (double) BUFFER_SIZE_BYTES);
    int remainder = (int) (difference % BUFFER_SIZE_BYTES);

    long lines = 0;
    for (int i = 0; i < quotient; i++) {
      lines += countLinesInChunk(is, BUFFER_SIZE_BYTES, false); // can be zero
    }
    if (remainder > 0) {
      lines += countLinesInChunk(is, remainder, true); // at least one
    }

    return lines;
  }

  private static long countLinesInChunk(DataInputStream is, int size, boolean lastChunk) {
    val buffer = readBuffer(is, size);

    checkState(!lastChunk || buffer[size - 1] == '\n', "expected '\\n' instead of %s for last chunk: %s",
        buffer[size - 1], new String(buffer));

    long lines = 0;
    for (int i = 0; i < size; i++) {
      // Simply ignore '\r'
      if (buffer[i] == '\n') {
        lines++;
      }
    }

    return lines;
  }

  private static List<Long> sortOffsets(Collection<Long> offsets) {
    val sortedOffsets = Lists.newArrayList(offsets);
    sort(sortedOffsets);

    return sortedOffsets;
  }

  private static byte[] readBuffer(DataInputStream inputStream, int size) {
    val buffer = new byte[size];

    try {
      inputStream.readFully(buffer);
    } catch (IOException e) {
      throw new RuntimeException("Error reading " + size + " bytes into buffer: " + Arrays.toString(buffer), e);
    }

    return buffer;
  }

  private static DataInputStream createInputStream(Path file) {
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

  private static boolean isHdfs() {
    val scheme = fileSystem.getScheme();

    return scheme.equals("hdfs");
  }

}
