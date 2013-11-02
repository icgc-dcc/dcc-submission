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

import static org.icgc.dcc.submission.validation.cascading.TupleState.createTupleError;

import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;

import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;
import org.icgc.dcc.submission.validation.core.ErrorType;

@Value
@RequiredArgsConstructor
public class SubmissionReportContext implements ReportContext {

  @NonNull
  SubmissionReport submissionReport;

  public SubmissionReportContext() {
    this(new SubmissionReport());
  }

  @Override
  public void reportError(String fileName, long lineNumber, String columnName, Object value, ErrorType type,
      Object... params) {
    val tupleError = createTupleError(type, columnName, value, lineNumber, params);
    addErrorTuple(fileName, tupleError);
  }

  @Override
  public void reportError(String fileName, long lineNumber, Object value, ErrorType type, Object... params) {
    reportError(fileName, lineNumber, null, value, type, params);
  }

  @Override
  public void reportError(String fileName, Object value, ErrorType type, Object... params) {
    reportError(fileName, -1, null, value, type, params);
  }

  @Override
  public void reportError(String fileName, ErrorType type, Object... params) {
    reportError(fileName, -1, null, null, type, params);
  }

  @Override
  public void reportError(String fileName, ErrorType type) {
    reportError(fileName, -1, null, null, type, new Object[] {});
  }

  private void addErrorTuple(String fileName, TupleError tupleError) {
    val schemaReport = resolveSchemaReport(fileName);
    addErrorTuple(schemaReport, tupleError);
  }

  private void addErrorTuple(SchemaReport schemaReport, TupleState.TupleError tupleError) {
    List<ErrorReport> errorReports = schemaReport.getErrors();
    for (ErrorReport errorReport : errorReports) {
      if (errorReport.getErrorType() == tupleError.getType()) {
        errorReport.updateReport(tupleError);
        return;
      }
    }

    ErrorReport errorReport = new ErrorReport(tupleError);
    schemaReport.addError(errorReport);
  }

  private SchemaReport resolveSchemaReport(String fileName) {
    SchemaReport schemaReport = submissionReport.getSchemaReport(fileName);
    if (schemaReport == null) {
      schemaReport = new SchemaReport();
      schemaReport.setName(fileName);

      submissionReport.addSchemaReport(schemaReport);
    }

    return schemaReport;
  }

}
