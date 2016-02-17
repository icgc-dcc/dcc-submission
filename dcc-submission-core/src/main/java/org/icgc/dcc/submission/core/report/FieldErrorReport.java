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
package org.icgc.dcc.submission.core.report;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.mongodb.morphia.annotations.Embedded;

/**
 * Reports on cell values within a column. Keeps track of the line, value and total count.
 * <p>
 * Example:
 * 
 * <pre>
 *  {
 *    "fieldNames": [ "f1" ],
 *    "parameters" : {
 *      ...
 *    },
 *    "count": "10,
 *    "lineNumbers": [ 10, 20, 30 ],
 *    "value": [ "v1", "v2", "v3" ]
 *  }
 * </pre>
 */
@Data
@Embedded
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = { "fieldNames" })
public class FieldErrorReport implements Serializable {

  /**
   * Maximum number of errors to store.
   * <p>
   * Does not control error count.
   */
  public static final int MAXIMUM_NUM_STORED_ERRORS = 50;

  /**
   * Key.
   */
  private List<String> fieldNames;

  /**
   * Description.
   */
  private Map<ErrorParameterKey, Object> parameters;

  /**
   * Values.
   */
  private long count;
  private List<Long> lineNumbers = newLinkedList();
  private List<Object> values = newLinkedList();

  public FieldErrorReport(@NonNull List<String> fieldNames, @NonNull Map<ErrorParameterKey, Object> parameters) {
    this.fieldNames = fieldNames;

    this.parameters = parameters;
  }

  public FieldErrorReport(@NonNull FieldErrorReport fieldErrorReport) {
    this.fieldNames = fieldErrorReport.fieldNames;

    this.parameters = newHashMap(fieldErrorReport.parameters);

    this.count = fieldErrorReport.count;
    this.lineNumbers = newArrayList(fieldErrorReport.lineNumbers);
    this.values = newArrayList(fieldErrorReport.values);
  }

  public boolean reportsOn(@NonNull Error error) {
    return fieldNames.equals(error.getFieldNames());
  }

  public void addError(@NonNull Error error) {
    // Always increment count
    incrementCount();

    // Only store if their is capacity
    if (isStorable()) {
      addValue(error.getValue());
      addLineNumber(error.getLineNumber());
    }
  }

  /**
   * Temporary: see DCC-2085, remove if/when unused.
   */
  public void addParameter(ErrorParameterKey key, Object value) {
    if (parameters == null) {
      parameters = newHashMap();
    }

    parameters.put(key, value);
  }

  private boolean isStorable() {
    return count <= MAXIMUM_NUM_STORED_ERRORS;
  }

  private void incrementCount() {
    count++;
  }

  private void addLineNumber(Long lineNumber) {
    lineNumbers.add(lineNumber);
  }

  private void addValue(Object value) {
    values.add(value);
  }

}