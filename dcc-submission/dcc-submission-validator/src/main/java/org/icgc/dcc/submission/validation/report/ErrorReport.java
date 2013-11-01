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
package org.icgc.dcc.submission.validation.report;

import static com.google.common.collect.Lists.newLinkedList;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;
import org.icgc.dcc.submission.validation.core.ErrorCode;

import com.google.common.collect.Lists;

@NoArgsConstructor
@Getter
@Setter(PRIVATE)
@ToString
public class ErrorReport implements Serializable {

  /**
   * The maximum number of {@link ColumnErrorReport} {@code line} and {@code value} additions that will be accepted.
   * Intended to limit reporting for the user.
   */
  public static final int MAX_ERROR_COUNT = 50;

  private ErrorCode errorType;
  private String description;
  private final List<ColumnErrorReport> columns = newLinkedList();

  public ErrorReport(TupleError error) {
    this.setErrorType(error.getCode());
    this.setDescription(error.getMessage());
    this.addColumn(error);
  }

  public boolean hasColumn(List<String> columnNames) {
    return this.getColumnByName(columnNames) != null;
  }

  public ColumnErrorReport getColumnByName(List<String> columnNames) {
    ColumnErrorReport column = null;
    for (ColumnErrorReport c : this.getColumns()) {
      if (c.getColumnNames().equals(columnNames)) {
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
    if (column.getCount() < MAX_ERROR_COUNT) {
      column.addLine(error.getLine());
      column.addValue(error.getValue());
    }

    column.incrementCount();
  }

  public void updateReport(TupleError error) {
    if (this.hasColumn(error.getColumnNames()) == true) {
      this.updateColumn(error);
    } else {
      this.addColumn(error);
    }
  }

  public void updateLineNumbers(Path file) throws IOException {
    Collection<Long> offsets = new HashSet<Long>();
    for (ColumnErrorReport column : this.columns) {
      offsets.addAll(column.getLines());
    }
    Map<Long, Long> byteToLine = ByteOffsetToLineNumber.convert(file, offsets);

    if (byteToLine != null) {
      for (ColumnErrorReport column : this.columns) {
        List<Long> newLines = Lists.newLinkedList();
        for (Long oldLine : column.getLines()) {
          newLines.add(byteToLine.get(oldLine));
        }
        column.setLines(newLines);
      }
    }

  }

}
