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
package org.icgc.dcc.submission.validation.first.file;

import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.MISSING_ROWS_ERROR;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.first.core.RowChecker;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RowCountChecker extends DelegatingFileRowChecker {

  private int lineCount;

  public RowCountChecker(RowChecker rowChecker, boolean failFast) {
    super(rowChecker, failFast);
  }

  public RowCountChecker(RowChecker rowChecker) {
    this(rowChecker, false);
  }

  @Override
  void performSelfCheck(String fileName, FileSchema fileSchema, CharSequence line, long lineNumber) {
    // Keep track of how many lines we've seen
    lineCount++;
  }

  @Override
  void performSelfFinish(String fileName, FileSchema fileSchema) {
    val headerCount = 1;
    val rowCount = lineCount - headerCount;

    val noRows = rowCount == 0;
    if (noRows) {
      log.info("No rows in file: '{}'", fileName);

      reportError(error()
          .fileName(fileName)
          .type(MISSING_ROWS_ERROR)
          .build());
    }

  }

}
