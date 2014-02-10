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

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Maps.difference;
import static com.google.common.collect.Sets.newTreeSet;
import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.ANY;
import static org.codehaus.jackson.annotate.JsonAutoDetect.Visibility.NONE;

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.model.SubmissionFile;
import org.icgc.dcc.submission.core.report.visitor.AddErrorReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.AddFieldReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.AddFileReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.AddSummaryReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.ErrorCountReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.GetFilesReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.IsValidReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.RefreshStateReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.RemoveFileReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.ResetReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.SetStateReportVisitor;
import org.icgc.dcc.submission.core.state.State;
import org.icgc.dcc.submission.core.util.TypeConverters.DataTypeConverter;
import org.icgc.dcc.submission.core.util.TypeConverters.FileTypeConverter;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Embedded;

import com.google.common.collect.ImmutableMap;

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
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
@Converters({ FileTypeConverter.class, DataTypeConverter.class })
public class Report implements ReportElement {

  private Set<DataTypeReport> dataTypeReports = newTreeSet();

  public Report(@NonNull Iterable<SubmissionFile> submissionFiles) {
    this(transformFiles(submissionFiles));
  }

  public Report(@NonNull Map<String, FileType> files) {
    updateFiles(files);
  }

  public Report(@NonNull Report report) {
    for (val dataTypeReport : report.getDataTypeReports()) {
      dataTypeReports.add(new DataTypeReport(dataTypeReport));
    }
  }

  @Override
  public void accept(@NonNull ReportVisitor visitor) {
    // Children first
    for (val dataTypeReport : dataTypeReports) {
      dataTypeReport.accept(visitor);
    }

    // Self last
    visitor.visit(this);
  }

  public void addDataTypeReport(@NonNull DataTypeReport dataTypeReport) {
    dataTypeReports.add(dataTypeReport);
  }

  public void removeDataTypeReport(@NonNull DataTypeReport dataTypeReport) {
    dataTypeReports.remove(dataTypeReport);
  }

  public void addSummary(@NonNull String fileName, @NonNull String name, @NonNull String value) {
    executeVisitor(new AddSummaryReportVisitor(fileName, name, value));
  }

  public void addFieldReport(@NonNull String fileName, @NonNull FieldReport fieldReport) {
    executeVisitor(new AddFieldReportVisitor(fileName, fieldReport));
  }

  public void addError(@NonNull Error error) {
    executeVisitor(new AddErrorReportVisitor(error));
  }

  public Map<String, FileType> getFiles() {
    return executeVisitor(new GetFilesReportVisitor()).getFiles();
  }

  public void updateFiles(@NonNull Iterable<SubmissionFile> submissionFiles) {
    updateFiles(transformFiles(submissionFiles));
  }

  public void updateFiles(@NonNull Map<String, FileType> newFiles) {
    val files = getFiles();

    val difference = difference(files, newFiles);
    val added = difference.entriesOnlyOnRight();
    val removed = difference.entriesOnlyOnLeft();

    for (val entry : added.entrySet()) {
      addFile(entry.getValue(), entry.getKey());
    }

    for (val entry : removed.entrySet()) {
      removeFile(entry.getValue(), entry.getKey());
    }
  }

  public void addFile(@NonNull FileType fileType, @NonNull String fileName) {
    executeVisitor(new AddFileReportVisitor(fileName, fileType));
  }

  public void removeFile(@NonNull FileType fileType, @NonNull String fileName) {
    executeVisitor(new RemoveFileReportVisitor(fileName, fileType));
  }

  public int getErrorCount() {
    return executeVisitor(new ErrorCountReportVisitor()).getErrorCount();
  }

  public boolean hasErrors() {
    return getErrorCount() > 0;
  }

  public boolean isValid() {
    return executeVisitor(new IsValidReportVisitor()).isValid();
  }

  public void reset(DataType... dataTypes) {
    reset(copyOf(dataTypes));
  }

  public void reset(@NonNull Iterable<DataType> dataTypes) {
    executeVisitor(new ResetReportVisitor(dataTypes));
  }

  public void setState(@NonNull State state, @NonNull Iterable<DataType> dataTypes) {
    executeVisitor(new SetStateReportVisitor(state, dataTypes));
  }

  public void refreshState() {
    executeVisitor(new RefreshStateReportVisitor());
  }

  private static Map<String, FileType> transformFiles(Iterable<SubmissionFile> submissionFiles) {
    val files = ImmutableMap.<String, FileType> builder();
    for (val submissionFile : submissionFiles) {
      val managed = submissionFile.getFileType() != null;
      if (managed) {
        files.put(submissionFile.getName(), submissionFile.getFileType());
      }
    }

    return files.build();
  }

  /**
   * Allows chaining for client ease of use.
   * 
   * @param visitor the visitor to execute
   * @return
   */
  private <T extends ReportVisitor> T executeVisitor(T visitor) {
    accept(visitor);

    return visitor;
  }

}
