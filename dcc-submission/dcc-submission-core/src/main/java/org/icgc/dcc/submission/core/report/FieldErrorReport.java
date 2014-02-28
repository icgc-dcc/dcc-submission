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
package org.icgc.dcc.submission.core.report;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
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
public class FieldErrorReport implements Serializable {

  /**
   * Description
   */
  private List<String> fieldNames;
  private Map<ErrorParameterKey, Object> parameters;

  /**
   * Values
   */
  private long count;
  private List<Long> lineNumbers = newLinkedList();
  private List<Object> values = newLinkedList();

  public FieldErrorReport(@NonNull Error error) {
    this.setFieldNames(error.getFieldNames());
    this.setParameters(error.getType().build(error.getParams()));

    this.setCount(1L);
    this.addValue(error.getValue());
    this.addLineNumber(error.getLineNumber());
  }

  public FieldErrorReport(@NonNull FieldErrorReport fieldErrorReport) {
    this.fieldNames = fieldErrorReport.fieldNames;
    this.parameters = newHashMap(fieldErrorReport.parameters);
    this.count = fieldErrorReport.count;

    this.lineNumbers = newArrayList(fieldErrorReport.lineNumbers);
    this.values = newArrayList(fieldErrorReport.values);
  }

  public void incrementCount() {
    count++;
  }

  public void addLineNumber(Long lineNumber) {
    lineNumbers.add(lineNumber);
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

  public void addValue(Object value) {
    values.add(value);
  }

}