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
package org.icgc.dcc.submission.core.report;

import static com.google.common.collect.Sets.newTreeSet;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.model.SubmissionDataType;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.submission.core.model.SubmissionFile;
import org.icgc.dcc.submission.core.report.visitor.AddErrorReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.AddFieldReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.AddFileReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.AddSummaryReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.ErrorCountReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.RemoveFileReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.ResetReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.SetStateReportVisitor;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.mongodb.morphia.annotations.Embedded;

/**
 * Represents a validation report for a submission within a release.
 * <p>
 * Example:
 * 
 * <pre>
 *  {
 *    "dataTypeReports": [ {
 *      ...
 *    } ]
 *  }
 * </pre>
 */
@Data
@Embedded
@NoArgsConstructor
@AllArgsConstructor
public class Report implements ReportElement {

  private Set<DataTypeReport> dataTypeReports = newTreeSet();

  public Report(@NonNull Iterable<SubmissionFile> submissionFiles) {
    for (val submissionFile : submissionFiles) {
      val fileName = submissionFile.getName();
      val fileType = SubmissionFileType.from(submissionFile.getDataType());

      addFile(fileType, fileName);
    }
  }

  public Report(@NonNull Report report) {
    for (val dataTypeReport : report.getDataTypeReports()) {
      dataTypeReports.add(new DataTypeReport(dataTypeReport));
    }
  }

  @Override
  public void accept(@NonNull ReportVisitor visitor) {
    for (val dataTypeReport : dataTypeReports) {
      dataTypeReport.accept(visitor);
    }

    visitor.visit(this);
  }

  public void addDataTypeReport(@NonNull DataTypeReport dataTypeReport) {
    dataTypeReports.add(dataTypeReport);
  }

  public void removeDataTypeReport(@NonNull DataTypeReport dataTypeReport) {
    dataTypeReports.remove(dataTypeReport);
  }

  public void addSummary(@NonNull String fileName, @NonNull String name, @NonNull String value) {
    accept(new AddSummaryReportVisitor(fileName, name, value));
  }

  public void addFieldReport(@NonNull String fileName, @NonNull FieldReport fieldReport) {
    accept(new AddFieldReportVisitor(fileName, fieldReport));
  }

  public void addError(@NonNull Error error) {
    accept(new AddErrorReportVisitor(error));
  }

  public void addFile(@NonNull SubmissionFileType fileType, @NonNull String fileName) {
    accept(new AddFileReportVisitor(fileName, fileType));
  }

  public void removeFile(@NonNull SubmissionFileType fileType, @NonNull String fileName) {
    accept(new RemoveFileReportVisitor(fileName, fileType));
  }

  public int getErrorCount() {
    val visitor = new ErrorCountReportVisitor();
    accept(visitor);

    return visitor.getErrorCount();
  }

  public boolean hasErrors() {
    return getErrorCount() > 0;
  }

  public void reset() {
    val all = Collections.<SubmissionDataType> emptySet();
    reset(all);
  }

  public void reset(@NonNull Collection<SubmissionDataType> dataTypes) {
    accept(new ResetReportVisitor(dataTypes));
  }

  public void setState(@NonNull SubmissionState state, @NonNull Collection<SubmissionDataType> dataTypes) {
    accept(new SetStateReportVisitor(state, dataTypes));
  }

}
