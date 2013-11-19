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
package org.icgc.dcc.submission.validation.core;

import static com.google.common.collect.Lists.newLinkedList;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;
import org.icgc.dcc.submission.validation.primary.report.ByteOffsetToLineNumber;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@NoArgsConstructor
@Getter
@Setter(PRIVATE)
@ToString
@Slf4j
public class ErrorReport implements Serializable {

  /**
   * The maximum number of {@link ColumnErrorReport} {@code line} and {@code value} additions that will be accepted.
   * Intended to limit reporting for the user.
   */
  public static final int MAX_ERROR_COUNT = 50;

  private ErrorType errorType;
  private int number;
  private String description;
  private final List<ColumnErrorReport> columns = newLinkedList();

  /**
   * Temporary band-aid to fix the issue of bite offsets being converted twice (see DCC-1908).
   */
  private boolean alreadyConverted = false;

  public ErrorReport(TupleError error) {
    this.setErrorType(error.getType());
    this.setNumber(error.getNumber());
    this.setDescription(error.getMessage());
    this.addColumn(error);
  }

  public boolean hasColumn(List<String> columnNames) {
    return getColumnByName(columnNames) != null;
  }

  public ColumnErrorReport getColumnByName(List<String> columnNames) {
    for (val column : getColumns()) {
      if (column.getColumnNames().equals(columnNames)) {
        return column;
      }
    }

    return null;
  }

  public void addColumn(TupleError error) {
    val column = new ColumnErrorReport(error);

    columns.add(column);
  }

  public void updateColumn(TupleError error) {
    val column = getColumnByName(error.getColumnNames());

    // Append line/value to lines/values
    if (column.getCount() < MAX_ERROR_COUNT) {
      column.addLine(error.getLine());
      column.addValue(error.getValue());
    }

    column.incrementCount();
  }

  public void updateReport(TupleError error) {
    if (hasColumn(error.getColumnNames())) {
      updateColumn(error);
    } else {
      addColumn(error);
    }
  }

  public void updateLineNumbers(Path file) throws IOException {
    if (alreadyConverted) {
      log.info("Skipping attempt to convert byte-offsets since it has already been done (see DCC-1908)");
    } else {
      val offsets = Sets.<Long> newHashSet();
      for (val column : columns) {
        offsets.addAll(column.getLines());
      }

      val byteToLine = ByteOffsetToLineNumber.convert(file, offsets);

      if (byteToLine != null) {
        for (val column : columns) {
          val newLines = Lists.<Long> newLinkedList();
          for (val oldLine : column.getLines()) {
            newLines.add(byteToLine.get(oldLine));
          }

          column.setLines(newLines);
        }
      }
      alreadyConverted = true;
    }
  }

}
