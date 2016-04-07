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
package org.icgc.dcc.submission.core.parser;

import static com.google.common.base.Preconditions.checkState;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.icgc.dcc.common.hadoop.parser.AbstractFileLineParser;
import org.icgc.dcc.submission.core.util.FieldNameComparator;
import org.icgc.dcc.submission.dictionary.model.FileSchema;

import com.google.common.collect.ImmutableSortedMap;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.val;

/**
 * Parser that guarantees {@link FileSchema} defined field ordering of its parsed {@code Map<String, String>} field map.
 */
@ToString
@RequiredArgsConstructor
public class FileLineMapParser extends AbstractFileLineParser<Map<String, String>> {

  /**
   * Dependencies.
   */
  @NonNull
  protected final FileSchema schema;
  @NonNull
  protected final Comparator<String> fieldNameComparator;

  public FileLineMapParser(FileSchema schema) {
    this.schema = schema;
    this.fieldNameComparator = new FieldNameComparator(schema.getFieldNames());
  }

  @Override
  public Map<String, String> parse(String line) {
    try {
      val fieldNames = schema.getFieldNames();
      val fieldValues = split(line);

      val actualCount = fieldValues.size();
      val expectedCount = fieldNames.size();
      checkState(actualCount == expectedCount,
          "Mismatched actual number of fields (%s) with expected number of fields (%s) parsing actual values %s with expected %s fields %s",
          actualCount, expectedCount, fieldValues, schema.getName(), fieldNames);

      return createRecord(fieldNames, fieldValues);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse line: " + line, e);
    }
  }

  private SortedMap<String, String> createRecord(List<String> fieldNames, List<String> fieldValues) {
    val record = createFieldOrderMapBuilder();

    for (int i = 0; i < fieldNames.size(); i++) {
      val fieldName = fieldNames.get(i);
      val fieldValue = fieldValues.get(i);

      record.put(fieldName, fieldValue);
    }

    return record.build();
  }

  private ImmutableSortedMap.Builder<String, String> createFieldOrderMapBuilder() {
    return ImmutableSortedMap.<String, String> orderedBy(fieldNameComparator);
  }

  protected static List<String> split(String line) {
    return FIELD_SPLITTER.splitToList(line);
  }

}
