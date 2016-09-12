/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.loader.record;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.submission.loader.util.DatabaseFields.PROJECT_ID_FIELD_NAME;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.util.Splitters;
import org.icgc.dcc.submission.loader.core.SubmissionLoaderException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@Slf4j
public class RecordReader implements Closeable, Iterator<Map<String, String>> {

  /**
   * Dependencies.
   */
  private final BufferedReader reader;

  /**
   * State.
   */
  private final List<String> fieldNames;
  private Map<String, String> nextDoc;

  @SneakyThrows
  public RecordReader(@NonNull BufferedReader reader) {
    this.reader = reader;
    // First line must be the header.
    this.fieldNames = resolveFieldNames(reader.readLine());
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }

  @Override
  public boolean hasNext() {
    if (nextDoc != null) {
      return true;
    }

    nextDoc = readRecord();

    return nextDoc != null;
  }

  @Override
  public Map<String, String> next() {
    if (nextDoc != null) {
      val next = nextDoc;
      nextDoc = null;

      return next;
    }

    return readRecord();
  }

  private Map<String, String> readRecord() {
    String line = null;
    try {
      line = reader.readLine();
      if (line == null) {
        return null;
      }
      return convertLine(line);
    } catch (Exception e) {
      throw new SubmissionLoaderException(e, "Failed to read record. \nField names: {}; \nLine: {}", fieldNames, line);
    }
  }

  private Map<String, String> convertLine(String line) {
    log.debug("Converting line:\n{}", line);
    val values = Splitters.TAB.splitToList(line);
    checkState(values.size() == fieldsCount(), "Failed to convert line. It has different number of fields. "
        + "Expected %s, found %s. %n%s", fieldsCount(), values.size(), line);

    val recordBuilder = ImmutableMap.<String, String> builder();
    for (int i = 0; i < fieldsCount(); i++) {
      val fieldName = fieldNames.get(i);
      val fieldValue = values.get(i);
      recordBuilder.put(fieldName, fieldValue);
    }

    try {
      return recordBuilder.build();
    } catch (Exception e) {
      log.error("Failed to convert line:\n{}\n{}", line, e);
      return Collections.emptyMap();
    }
  }

  // Doesn't include Project ID
  private int fieldsCount() {
    return fieldNames.size() - 1;
  }

  private static List<String> resolveFieldNames(String header) {
    checkNotNull(header, "Malformed file has no header.");

    val fieldNamesBuilder = ImmutableList.<String> builder();
    fieldNamesBuilder.addAll(Splitters.TAB.split(header));
    fieldNamesBuilder.add(PROJECT_ID_FIELD_NAME);

    val fieldNames = fieldNamesBuilder.build();
    log.debug("Field names: {}", fieldNames);

    return fieldNames;
  }

}
