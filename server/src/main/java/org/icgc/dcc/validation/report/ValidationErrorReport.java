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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.validation.ErrorParameterKey;
import org.icgc.dcc.validation.ValidationErrorCode;
import org.icgc.dcc.validation.cascading.TupleState.TupleError;

import com.google.common.collect.Lists;

public class ValidationErrorReport {

  private ValidationErrorCode errorType;

  private String description;

  private final List<ColumnErrorReport> columns = Lists.newLinkedList();

  private static final int MAX_ERROR_COUNT = 50;

  public ValidationErrorReport() {
  }

  public ValidationErrorReport(TupleError error) {
    this.setErrorType(error.getCode());
    this.setDescription(error.getMessage());
    this.addColumn(error);
  }

  public ValidationErrorCode getErrorType() {
    return errorType;
  }

  public String getDescription() {
    return description;
  }

  public List<ColumnErrorReport> getColumns() {
    return columns;
  }

  public boolean hasColumn(List<String> columnNames) {
    return this.getColumnByName(columnNames) != null;
  }

  public ColumnErrorReport getColumnByName(List<String> columnNames) {
    ColumnErrorReport column = null;
    for(ColumnErrorReport c : this.getColumns()) {
      if(c.getColumnNames().equals(columnNames)) {
        column = c;
        break;
      }
    }
    return column;
  }

  public void addColumn(TupleError error) {
    ColumnErrorReport column = new ColumnErrorReport(error);
    this.columns.add(column);
  }

  public void updateColumn(TupleError error) {
    ColumnErrorReport column = this.getColumnByName(error.getColumnNames());

    // Append line/value to lines/values
    if(column.getCount() < MAX_ERROR_COUNT) {
      column.addLine(error.getLine());
      column.addValue(error.getValue());
    }

    column.incCount();
  }

  public void updateReport(TupleError error) {
    if(this.hasColumn(error.getColumnNames()) == true) {
      this.updateColumn(error);
    } else {
      this.addColumn(error);
    }
  }

  private void setDescription(String description) {
    this.description = description;
  }

  private void setErrorType(ValidationErrorCode errorType) {
    this.errorType = errorType;
  }

  public void updateLineNumbers(Path file) throws IOException {
    Collection<Long> offsets = new HashSet<Long>();
    for(ColumnErrorReport column : this.columns) {
      offsets.addAll(column.getLines());
    }
    Map<Long, Long> byteToLine = ByteOffsetToLineNumber.convert(file, offsets);

    if(byteToLine != null) {
      for(ColumnErrorReport column : this.columns) {
        List<Long> newLines = Lists.newLinkedList();
        for(Long oldLine : column.getLines()) {
          newLines.add(byteToLine.get(oldLine).longValue());
        }
        column.setLines(newLines);
      }
    }

  }

  private static class ColumnErrorReport {
    private List<String> columnNames;

    private long count;

    private List<Long> lines = Lists.newLinkedList();

    private List<Object> values = Lists.newLinkedList();

    private Map<ErrorParameterKey, Object> parameters;

    public ColumnErrorReport() {
    }

    public ColumnErrorReport(TupleError error) {
      this.setColumnNames(error.getColumnNames());
      this.setCount(1L);
      this.lines.add(error.getLine());
      this.values.add(error.getValue());

      this.setParameters(error.getParameters());
    }

    public void incCount() {
      this.count++;
    }

    public long getCount() {
      return this.count;
    }

    public Map<ErrorParameterKey, Object> getParameters() {
      return this.parameters;
    }

    public List<String> getColumnNames() {
      return this.columnNames;
    }

    public List<Object> getValues() {
      return this.values;
    }

    public void addValue(Object value) {
      List<Object> values = this.getValues();
      values.add(value);
      this.setValues(values);
    }

    public List<Long> getLines() {
      return this.lines;
    }

    public void addLine(Long line) {
      List<Long> lines = this.getLines();
      lines.add(line);
      this.setLines(lines);
    }

    private void setValues(List<Object> values) {
      this.values = values;
    }

    private void setLines(List<Long> lines) {
      this.lines = lines;
    }

    private void setCount(long c) {
      this.count = c;
    }

    private void setColumnNames(List<String> columnNames) {
      this.columnNames = columnNames;
    }

    private void setParameters(Map<ErrorParameterKey, Object> params) {
      this.parameters = params;
    }
  }
}
