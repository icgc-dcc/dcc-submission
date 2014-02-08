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
package org.icgc.dcc.submission.validation.core;

import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import lombok.experimental.NonFinal;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.core.report.Error;
import org.icgc.dcc.submission.core.report.ErrorReport;
import org.icgc.dcc.submission.core.report.FieldReport;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.core.report.SummaryReport;
import org.icgc.dcc.submission.validation.primary.report.ByteOffsetToLineNumber;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Wraps and "adapts" a {@link Report}.
 */
@Value
@RequiredArgsConstructor
public class DefaultReportContext {

  /**
   * State.
   */
  @NonNull
  Report report;

  /**
   * Total number of errors encountered at a point in time.
   */
  @NonFinal
  int errorCount;

  public DefaultReportContext() {
    this(new Report());
  }

  public void reportSummary(String fileName, String name, String value) {
    val fileReport = report.getFileReport(fileName).get();
    val summaryReport = new SummaryReport(name, value);

    fileReport.addSummaryReport(summaryReport);
  }

  public void reportField(String fileName, FieldReport fieldReport) {
    val fileReport = report.getFileReport(fileName).get();
    fileReport.addFieldReport(fieldReport);
  }

  public void reportError(Error error) {
    errorCount++;
    val fileReport = report.getFileReport(error.getFileName()).get();

    fileReport.addError(error);
  }

  public boolean hasErrors() {
    return errorCount > 0;
  }

  public void reportLineNumbers(Path filePath) {
    // Shorthands
    val fileName = filePath.getName();
    val fileReport = report.getFileReport(fileName).get();

    for (val errorReport : fileReport.getErrorReports()) {
      if (!errorReport.isConverted()) {
        // Convert byte offsets to line numbers
        val mapping = getByteOffsetLineNumberMapping(filePath, errorReport);
        updateLineNumbers(errorReport, mapping);

        // Remember we converted so that we don't do again
        errorReport.setConverted(true);
      }
    }
  }

  private static void updateLineNumbers(ErrorReport errorReport, Map<Long, Long> mapping) {
    for (val fieldErrorReport : errorReport.getFieldErrorReports()) {
      val newLineNumbers = Lists.<Long> newLinkedList();
      for (val byteOffset : fieldErrorReport.getLineNumbers()) {
        newLineNumbers.add(mapping.get(byteOffset));
      }

      fieldErrorReport.setLineNumbers(newLineNumbers);
    }
  }

  private static Map<Long, Long> getByteOffsetLineNumberMapping(Path filePath, ErrorReport errorReport) {
    val offsets = Sets.<Long> newHashSet();

    val fieldErrorReports = errorReport.getFieldErrorReports();
    for (val fieldErrorReport : fieldErrorReports) {
      offsets.addAll(fieldErrorReport.getLineNumbers());
    }

    return ByteOffsetToLineNumber.convert(filePath, offsets);
  }

}
