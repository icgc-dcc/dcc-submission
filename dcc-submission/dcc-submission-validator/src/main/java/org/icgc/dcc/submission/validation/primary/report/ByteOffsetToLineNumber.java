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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

@Slf4j
public class ByteOffsetToLineNumber {// TODO: make non-static class

  private static final int BUFFER_SIZE = 1000000; // in bytes

  @Inject
  protected static FileSystem fileSystem;

  public static Map<Long, Long> convert(@NonNull Path file, @NonNull Collection<Long> offsets) {
    return convert(file, offsets, true);
  }

  public static Map<Long, Long> convert(@NonNull Path file, @NonNull Collection<Long> offsets, boolean check) {
    if (check && !isHdfs()) {
      log.info("Local filesystem: not remapping line numbers for path: {}" + file);
      return null;
    }

    log.info("Hdfs: remapping line numbers for path " + file.toString());
    checkNotNull(fileSystem);

    // Need to sort offsets to ensure correct iteration order
    val sortedOffsets = sortOffsets(offsets);
    log.info("Offsets: {}", sortedOffsets);

    val byteToLineOffsetMap = buildByteToLineOffsetMap(file, sortedOffsets);

    return byteToLineOffsetMap;
  }

  @SneakyThrows
  private static Map<Long, Long> buildByteToLineOffsetMap(Path file, List<Long> sortedOffsets) {
    @Cleanup
    val inputStream = createInputStream(file);

    val offsetMap = Maps.<Long, Long> newLinkedHashMap();
    long previousOffset = 0;
    long lineOffset = 1; // 1-based

    for (Long byteOffset : sortedOffsets) {
      long currentOffset = byteOffset.longValue();
      if (currentOffset == -1L) {
        // File level error maybe? Dunno...
        offsetMap.put(-1L, -1L);
      } else {
        checkState(currentOffset >= 0, "Current offset is negative: %s", currentOffset);
        checkState(currentOffset > previousOffset,
            "Current offset %s is greater than previous offset %s", currentOffset, previousOffset); // no two same
                                                                                                    // offsets

        lineOffset += countLinesInInterval(inputStream, previousOffset, currentOffset);
        offsetMap.put(byteOffset, lineOffset);

        previousOffset = byteOffset;
      }
    }

    return offsetMap;
  }

  private static final long countLinesInInterval(DataInputStream is, long previousOffset, long currentOffset) {
    long difference = currentOffset - previousOffset;
    long quotient = (long) Math.floor(difference / (double) BUFFER_SIZE);
    int remainder = (int) (difference % BUFFER_SIZE);

    long lines = 0;
    for (int i = 0; i < quotient; i++) {
      lines += countLinesInChunk(is, BUFFER_SIZE, false); // can be zero
    }
    if (remainder > 0) {
      lines += countLinesInChunk(is, remainder, true); // at least one
    }

    return lines;
  }

  private static final long countLinesInChunk(DataInputStream is, int size, boolean lastChunk) {
    byte[] buffer = readBuffer(is, size);
    if (lastChunk && buffer[size - 1] != '\n') {
      throw new RuntimeException( // TODO: replace with our own
          String.format("expected '\\n' instead of %s for last chunk: %s", buffer[size - 1], new String(buffer)));
    }

    long lines = 0;
    for (int i = 0; i < size; i++) {
      if (buffer[i] == '\n') { // simply ignore '\r'
        lines++;
      }
    }
    return lines;
  }

  private static List<java.lang.Long> sortOffsets(Collection<Long> offsets) {
    val sortedOffsets = new ArrayList<Long>(offsets);
    Collections.sort(sortedOffsets); // e.g. [11277, 11511, 11744, 11976, 32434, 32668, 32901, 33135]

    return sortedOffsets;
  }

  private static byte[] readBuffer(DataInputStream is, int size) {
    byte[] buffer = new byte[size];
    try {
      is.readFully(buffer);
    } catch (IOException e) {
      throw new RuntimeException("Error reading " + size + " bytes into buffer: " + Arrays.toString(buffer));
    }

    return buffer;
  }

  private static DataInputStream createInputStream(Path file) {
    Configuration conf = fileSystem.getConf();
    CompressionCodecFactory factory = new CompressionCodecFactory(conf);

    try {
      CompressionCodec codec = factory.getCodec(file);
      InputStream inputStream =
          (codec == null) ? fileSystem.open(file) : codec.createInputStream(fileSystem.open(file));
      return new DataInputStream(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(file.toString());
    }
  }

  private static boolean isHdfs() {
    return fileSystem.getScheme().equals("hdfs");
  }

}
