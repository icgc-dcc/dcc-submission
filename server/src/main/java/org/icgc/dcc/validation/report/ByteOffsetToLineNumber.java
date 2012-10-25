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
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class ByteOffsetToLineNumber {

  private static final Logger log = LoggerFactory.getLogger(ByteOffsetToLineNumber.class);

  @Inject
  private static FileSystem fs;

  public static Map<Long, Integer> convert(Path file, Collection<Long> offsets) throws IOException {
    checkNotNull(file);
    checkNotNull(fs);

    if(fs.getScheme().equals("hdfs") == false) {
      log.info("Local filesystem: not remapping line numbers for path " + file.toString());
      return null;
    }

    log.info("Remapping byte offsets to line numbers for " + file.toString());
    List<Long> sortedOffsets = new ArrayList<Long>(offsets);
    Collections.sort(sortedOffsets);
    Map<Long, Integer> offsetToLine = new HashMap<Long, Integer>(sortedOffsets.size());

    LineNumberReader lineNumberReader = new LineNumberReader(new InputStreamReader(fs.open(file)));
    lineNumberReader.setLineNumber(1);
    long currentByte = 0;

    for(long offset : sortedOffsets) {
      while(currentByte < offset) {
        String line = lineNumberReader.readLine();
        if(line == null) {
          currentByte += 1; // if line is null, just +1 for line terminator
        } else {
          currentByte += line.getBytes().length + 1; // "+1" to account for line terminator
        }
      }
      offsetToLine.put(offset, lineNumberReader.getLineNumber());
    }

    return offsetToLine;
  }
}
