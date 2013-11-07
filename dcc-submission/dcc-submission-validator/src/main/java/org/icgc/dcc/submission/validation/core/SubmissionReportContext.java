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

import static com.google.common.base.Throwables.propagate;
import static org.icgc.dcc.submission.validation.cascading.TupleState.createTupleError;

import java.io.IOException;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.normalization.NormalizationReport;
import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;

/**
 * Wraps and "adapts" a {@link SubmissionReport}.
 */
@Value
@RequiredArgsConstructor
@Slf4j
public class SubmissionReportContext implements ReportContext {

  /**
   * Adaptee.
   */
  @NonNull
  SubmissionReport submissionReport;

  /**
   * Total number of errors encountered at a point in time.
   */
  @NonFinal
  int errorCount;

  public SubmissionReportContext() {
    this(new SubmissionReport());
  }

  @Override
  public void reportField(String fileName, FieldReport fieldReport) {
    addFieldReport(fileName, fieldReport);
  }

  @Override
  public void reportNormalization(String fileName, NormalizationReport normalizationReport) {
    log.info("Reporting: '{}'", normalizationReport); // TODO
  }

  @Override
  public void reportError(String fileName, TupleError tupleError) {
    addErrorTuple(fileName, tupleError);
  }

  @Override
  public void reportError(String fileName, long lineNumber, String columnName, Object value, ErrorType type,
      Object... params) {
    val tupleError = createTupleError(type, columnName, value, lineNumber, params);
    reportError(fileName, tupleError);
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

  @Override
  public boolean hasErrors() {
    return errorCount > 0;
  }

  @Override
  public void reportLineNumbers(Path path) {
    val schemaReport = submissionReport.getSchemaReport(path.getName());
    for (val errorReport : schemaReport.getErrors()) {
      try {
        errorReport.updateLineNumbers(path);
      } catch (IOException e) {
        log.error("Exception updating line numbers for: '{}'", path);
        propagate(e);
      }
    }
  }

  private void addErrorTuple(String fileName, TupleError tupleError) {
    errorCount++;

    val schemaReport = resolveSchemaReport(fileName);
    addErrorTuple(schemaReport, tupleError);
  }

  private void addErrorTuple(SchemaReport schemaReport, TupleError tupleError) {
    val errorReports = schemaReport.getErrors();
    for (val errorReport : errorReports) {
      val errorTypeExists = errorReport.getErrorType() == tupleError.getType();
      if (errorTypeExists) {
        // Reuse, no need to continue
        errorReport.updateReport(tupleError);

        return;
      }
    }

    // Seed on first use
    val errorReport = new ErrorReport(tupleError);
    schemaReport.addError(errorReport);
  }

  private void addFieldReport(String fileName, FieldReport fieldReport) {
    val schemaReport = resolveSchemaReport(fileName);
    addFieldReport(schemaReport, fieldReport);
  }

  private void addFieldReport(SchemaReport schemaReport, FieldReport fieldReport) {
    schemaReport.addFieldReport(fieldReport);
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
