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
package org.icgc.dcc.submission.core.report;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Sets.newTreeSet;
import static org.icgc.dcc.submission.core.report.FileState.getDefaultState;

import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.util.Serdes.FileTypeDeserializer;
import org.icgc.dcc.submission.core.util.Serdes.FileTypeSerializer;
import org.mongodb.morphia.annotations.Embedded;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Reports on a submission file.
 * <p>
 * Example:
 * 
 * <pre>
 *  {
 *    "fileName": "ssm_p.txt",
 *    "fileState": "NOT_VALIDATED",
 *    "summaryReports": [ {
 *      ...
 *    } ],
 *    "fieldReports": [ {
 *      ...
 *    } ]
 *    "errorReports": [ {
 *      ...
 *    } ]
 *  }
 * </pre>
 */
@Data
@Embedded
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "fileName")
public class FileReport implements ReportElement, Comparable<FileReport> {

  /**
   * Key.
   * <p>
   * The name of the file the report describes.
   */
  String fileName;

  /**
   * The type of the file the report describes.
   * <p>
   * Not strictly needed, but simplifies implementation (e.g. visitors).
   */
  @JsonSerialize(using = FileTypeSerializer.class)
  @JsonDeserialize(using = FileTypeDeserializer.class)
  FileType fileType;

  /**
   * The state of the file.
   */
  FileState fileState = getDefaultState();

  /**
   * Summary reports.
   */
  List<SummaryReport> summaryReports = newLinkedList();

  /**
   * Field reports.
   */
  List<FieldReport> fieldReports = newLinkedList();

  /**
   * Error Reports
   */
  Set<ErrorReport> errorReports = newTreeSet();

  public FileReport(@NonNull String fileName, @NonNull FileType fileType) {
    this.fileName = fileName;
    this.fileType = fileType;
  }

  public FileReport(@NonNull FileReport fileReport) {
    this.fileName = fileReport.fileName;
    this.fileType = fileReport.fileType;
    this.fileState = fileReport.fileState;

    for (val summaryReport : fileReport.summaryReports) {
      summaryReports.add(new SummaryReport(summaryReport));
    }

    for (val fieldReport : fileReport.fieldReports) {
      fieldReports.add(new FieldReport(fieldReport));
    }

    for (val errorReport : fileReport.errorReports) {
      errorReports.add(new ErrorReport(errorReport));
    }
  }

  @Override
  public void accept(@NonNull ReportVisitor visitor) {
    // Children first
    for (val errorReport : errorReports) {
      visitor.visit(errorReport);
    }

    // Self last
    visitor.visit(this);
  }

  public void addSummaryReport(@NonNull SummaryReport summaryReport) {
    summaryReports.add(summaryReport);
  }

  public void addFieldReport(@NonNull FieldReport fieldReport) {
    fieldReports.add(fieldReport);
  }

  public void addError(@NonNull Error error) {
    val errorReport = resolveErrorReport(error);

    errorReport.addError(error);
  }

  @Override
  public int compareTo(@NonNull FileReport other) {
    return fileName.compareTo(other.fileName);
  }

  private ErrorReport resolveErrorReport(Error error) {
    ErrorReport errorReport = getErrorReport(error);
    if (errorReport == null) {
      errorReport = new ErrorReport(error.getType(), error.getNumber(), error.getMessage());

      errorReports.add(errorReport);
    }

    return errorReport;
  }

  private ErrorReport getErrorReport(@NonNull Error error) {
    for (val errorReport : errorReports) {
      if (errorReport.reportsOn(error)) {
        return errorReport;
      }
    }

    return null;
  }

}
