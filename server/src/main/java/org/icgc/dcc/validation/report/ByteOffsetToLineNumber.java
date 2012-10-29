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
package org.icgc.dcc.validation.report;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class ByteOffsetToLineNumber {// TODO: make non-static class

  private static final Logger log = LoggerFactory.getLogger(ByteOffsetToLineNumber.class);

  private static final int BUFFER_SIZE = 1000000; // in bytes

  @Inject
  private static FileSystem fs;

  public static Map<Long, Integer> convert(Path file, Collection<Long> offsets) {
    checkNotNull(file);
    checkNotNull(fs);

    if(fs.getScheme().equals("hdfs") == false) {
      log.info("Local filesystem: not remapping line numbers for path " + file.toString());
      return null;
    }

    List<Long> sortedOffsets = new ArrayList<Long>(offsets);
    Collections.sort(sortedOffsets); // e.g. [11277, 11511, 11744, 11976, 32434, 32668, 32901, 33135]
    Map<Long, Integer> byteToLineOffsetMap = buildByteToLineOffsetMap(file, sortedOffsets);
    return byteToLineOffsetMap;
  }

  private static Map<Long, Integer> buildByteToLineOffsetMap(Path file, List<Long> sortedOffsets) {
    InputStream is = null;
    try {
      is = fs.open(file);
    } catch(IOException e) {
      throw new RuntimeException( // TODO: replace with our own
          String.format("%s", file));
    }

    Map<Long, Integer> offsetMap = Maps.newLinkedHashMap();

    long previousOffset = 0;
    int lineOffset = 1; // 1-based
    for(Long byteOffset : sortedOffsets) {
      int currentOffset = byteOffset.intValue(); // TODO: make non-static class out of it so as store those in members
                                                 // and log useful error messages
      Preconditions.checkState(currentOffset > previousOffset); // no two same offsets

      lineOffset += countLinesInInterval(is, previousOffset, currentOffset);
      offsetMap.put(byteOffset, lineOffset);

      previousOffset = byteOffset;
    }

    try {
      is.close();
    } catch(IOException e) {
      throw new RuntimeException( // TODO: replace with our own
          String.format("%s", file));
    }

    return offsetMap;
  }

  private static final int countLinesInInterval(InputStream is, long previousOffset, long currentOffset) {
    long difference = currentOffset - previousOffset;
    long quotient = difference / BUFFER_SIZE;
    int remainder = (int) (difference % BUFFER_SIZE);

    int lines = 0;
    for(int i = 0; i < quotient; i++) {
      lines += countLinesInChunk(is, BUFFER_SIZE, false); // can be zero
    }
    if(remainder > 0) {
      lines += countLinesInChunk(is, remainder, true); // at least one
    }

    return lines;
  }

  private static final long countLinesInChunk(InputStream is, int size, boolean lastChunk) {
    byte[] buffer = readBuffer(is, size);
    if(lastChunk && buffer[size - 1] != '\n') {
      throw new RuntimeException( // TODO: replace with our own
          String.format("expected '\\n' instead of %s for last chunk", buffer[size - 1]));
    }

    long lines = 0;
    for(int i = 0; i < size; i++) {
      if(buffer[i] == '\n') { // simply ignore '\r'
        lines++;
      }
    }
    return lines;
  }

  private static byte[] readBuffer(InputStream is, int size) {
    byte[] buffer = new byte[size];
    try {
      is.read(buffer);
    } catch(IOException e) {
      throw new RuntimeException(String.format("%s", size));
    }
    return buffer;
  }
}
