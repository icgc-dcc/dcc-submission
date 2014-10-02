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
package org.icgc.dcc.submission.validation.first.file;

import static org.icgc.dcc.core.util.Separators.TAB_CHARACTER;
import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.STRUCTURALLY_INVALID_ROW_ERROR;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.first.core.RowChecker;

@Slf4j
public class RowColumnChecker extends DelegatingFileRowChecker {

  public RowColumnChecker(RowChecker rowChecker, boolean failFast) {
    super(rowChecker, failFast);
  }

  public RowColumnChecker(RowChecker rowChecker) {
    this(rowChecker, false);
  }

  @Override
  void performSelfCheck(String fileName, FileSchema fileSchema, CharSequence line, long lineNumber) {
    val expectedNumColumns = getExpectedColumnCount(fileSchema);
    val actualNumColumns = getActualColumnCount(line);
    if (isCountMismatch(expectedNumColumns, actualNumColumns)) {
      log.info("Row does not match the expected number of columns: " + expectedNumColumns + ", actual: "
          + actualNumColumns + " at line " + lineNumber);

      reportError(error()
          .fileName(fileName)
          .lineNumber(lineNumber)
          .type(STRUCTURALLY_INVALID_ROW_ERROR)
          .value(actualNumColumns)
          .params(expectedNumColumns)
          .build());
    }
  }

  @Override
  void performSelfFinish(String fileName, FileSchema fileSchema) {
    // No-op
  }

  private int getExpectedColumnCount(FileSchema fileSchema) {
    return fileSchema.getFields().size(); // TODO: fix inefficiency
  }

  private int getActualColumnCount(CharSequence line) {
    int separatorCount = 0;
    for (int i = 0; i < line.length(); i++) {
      if (line.charAt(i) == TAB_CHARACTER) {
        separatorCount++;
      }
    }

    // One more field than separator count
    val fieldCount = separatorCount + 1;

    return fieldCount;
  }

  private boolean isCountMismatch(int expectedNumColumns, int actualNumColumns) {
    return actualNumColumns != expectedNumColumns;
  }

}
