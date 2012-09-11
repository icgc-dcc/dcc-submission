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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.validation.ValidationErrorCode;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.cascading.TupleState.TupleError;

/**
 * 
 */
public class ValidationErrorReport {

  private ValidationErrorCode errorType;

  private String description;

  private List<ColumnErrorReport> columns;

  private static final int MAX_ERROR_COUNT = 50;

  private static final String LINE = "line";

  private static final String COLUMN_NAME = "columnName";

  private static final String VALUE = "value";

  public ValidationErrorReport() {
  }

  /**
   * @param tupleState
   */
  public ValidationErrorReport(TupleState tupleState) {
    Iterator<TupleState.TupleError> errors = tupleState.getErrors().iterator();
    if(errors.hasNext()) {
      TupleState.TupleError error = errors.next();
      this.setErrorType(error.getCode());
      this.setDescription(error.getMessage());
      this.addColumn(error.getParameters());
    }
  }

  /**
   * @param error
   */
  public ValidationErrorReport(TupleError error) {
    this.columns = new LinkedList<ColumnErrorReport>();
    this.setErrorType(error.getCode());
    this.setDescription(error.getMessage());
    this.addColumn(error.getParameters());
  }

  /**
   * @return the errorType
   */
  public ValidationErrorCode getErrorType() {
    return errorType;
  }

  /**
   * @param errorType the errorType to set
   */
  public void setErrorType(ValidationErrorCode errorType) {
    this.errorType = errorType;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @param description the description to set
   */
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * @return the columns
   */
  public List<ColumnErrorReport> getColumns() {
    return columns;
  }

  /**
   * @param columns the columns to set
   */
  public void addColumn(Map<String, Object> parameters) {
    ColumnErrorReport column = new ColumnErrorReport(parameters);
    this.columns.add(column);
  }

  public void updateColumn(String columnName, TupleError error, int line) {
    ColumnErrorReport column = this.getColumnByName(columnName);
    column.incCount();

    // Append line/value to lines/values
    if(column.getCount() < MAX_ERROR_COUNT) {
      column.addLine(line);
      column.addValue(error.getParameters().get(VALUE));
    }
  }

  public ColumnErrorReport getColumnByName(String columnName) {
    ColumnErrorReport column = null;

    for(ColumnErrorReport c : this.getColumns()) {
      if(c.getColumnName().equals(columnName)) {
        column = c;
        break;
      }
    }

    return column;
  }

  public boolean hasColumn(String columnName) {
    return this.getColumnByName(columnName) != null ? true : false;
  }

  private static class ColumnErrorReport {
    private String columnName;

    private int count;

    private List<Integer> lines;

    private List<Object> values;

    private Map<String, Object> parameters;

    public ColumnErrorReport() {
    }

    public ColumnErrorReport(Map<String, Object> params) {
      this.lines = new LinkedList<Integer>();
      this.values = new LinkedList<Object>();

      this.setColumnName(params.get(COLUMN_NAME).toString());
      this.setCount(1);
      this.lines.add((Integer) params.get(LINE));
      this.values.add(params.get(VALUE));

      params.remove(LINE);
      params.remove(VALUE);
      params.remove(COLUMN_NAME);
      this.setParameters(params);
    }

    public void incCount() {
      this.setCount(this.getCount() + 1);
    }

    public int getCount() {
      return this.count;
    }

    public Map<String, Object> getParameters() {
      return this.parameters;
    }

    public String getColumnName() {
      return this.columnName;
    }

    public List<Object> getValues() {
      return this.values;
    }

    public void addValue(Object value) {
      List<Object> values = this.getValues();
      values.add(value);
      this.setValues(values);
    }

    public List<Integer> getLines() {
      return this.lines;
    }

    public void addLine(int line) {
      List<Integer> lines = this.getLines();
      lines.add(line);
      this.setLines(lines);
    }

    private void setValues(List<Object> values) {
      this.values = values;
    }

    private void setLines(List<Integer> lines) {
      this.lines = lines;
    }

    private void setCount(int c) {
      this.count = c;
    }

    private void setColumnName(String columnName) {
      this.columnName = columnName;
    }

    private void setParameters(Map<String, Object> params) {
      this.parameters = params;
    }

  }
}
