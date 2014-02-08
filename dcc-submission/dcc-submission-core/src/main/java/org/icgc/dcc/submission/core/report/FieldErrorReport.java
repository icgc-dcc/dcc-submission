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

import static com.google.common.collect.Lists.newLinkedList;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Reports on cell values within a column. Keeps track of the line, value and total count.
 */
@Data
public class FieldErrorReport implements Serializable {

  /**
   * Description
   */
  private List<String> fieldNames;
  private Map<ErrorParameterKey, Object> parameters;
  private long count;

  /**
   * Values
   */
  private List<Long> lineNumbers = newLinkedList();
  private List<Object> values = newLinkedList();

  public FieldErrorReport(Error error) {
    this.setFieldNames(error.getFieldNames());
    this.setCount(1L);

    this.addLineNumber(error.getLineNumber());
    this.addValue(error.getValue());
    this.setParameters(error.getType().build(error.getParams()));
  }

  public void incrementCount() {
    count++;
  }

  public void addLineNumber(Long lineNumber) {
    lineNumbers.add(lineNumber);
  }

  public void addValue(Object value) {
    values.add(value);
  }

}