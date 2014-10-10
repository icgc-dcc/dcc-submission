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

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.difference;
import static com.google.common.collect.Sets.newTreeSet;

import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.report.visitor.AbortVisitor;
import org.icgc.dcc.submission.core.report.visitor.AddErrorVisitor;
import org.icgc.dcc.submission.core.report.visitor.AddFieldVisitor;
import org.icgc.dcc.submission.core.report.visitor.AddFileVisitor;
import org.icgc.dcc.submission.core.report.visitor.AddSummaryVisitor;
import org.icgc.dcc.submission.core.report.visitor.ErrorCountVisitor;
import org.icgc.dcc.submission.core.report.visitor.GetFileReportVisitor;
import org.icgc.dcc.submission.core.report.visitor.GetFilesVisitor;
import org.icgc.dcc.submission.core.report.visitor.InheritStateVisitor;
import org.icgc.dcc.submission.core.report.visitor.IsValidVisitor;
import org.icgc.dcc.submission.core.report.visitor.RefreshStateVisitor;
import org.icgc.dcc.submission.core.report.visitor.RemoveFileVisitor;
import org.icgc.dcc.submission.core.report.visitor.ResetVisitor;
import org.icgc.dcc.submission.core.util.TypeConverters.DataTypeConverter;
import org.icgc.dcc.submission.core.util.TypeConverters.FileTypeConverter;
import org.icgc.dcc.submission.fs.SubmissionFile;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.mongodb.morphia.annotations.Converters;
import org.mongodb.morphia.annotations.Embedded;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

/**
 * Represents a validation report for a submission within a release. This is an "Aggregate Root" in the DDD sense.
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
 * 
 * @see http://martinfowler.com/bliki/DDD_Aggregate.html
 */
@Data
@Embedded
@NoArgsConstructor
@AllArgsConstructor
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, isGetterVisibility = NONE, setterVisibility = NONE)
@Converters({ FileTypeConverter.class, DataTypeConverter.class })
@Slf4j
public class Report implements ReportElement {

  private Set<DataTypeReport> dataTypeReports = newTreeSet();

  public Report(@NonNull Iterable<SubmissionFile> submissionFiles) {
    this(transformFiles(submissionFiles));
  }

  public Report(@NonNull Map<String, FileType> files) {
    refreshFiles(files);
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
    executeVisitor(new AddSummaryVisitor(fileName, name, value));
  }

  public void addFieldReport(@NonNull String fileName, @NonNull FieldReport fieldReport) {
    executeVisitor(new AddFieldVisitor(fileName, fieldReport));
  }

  public void addError(@NonNull Error error) {
    executeVisitor(new AddErrorVisitor(error));
  }

  public Map<String, FileType> getFiles() {
    return executeVisitor(new GetFilesVisitor()).getFiles();
  }

  public void refreshFiles(@NonNull Iterable<SubmissionFile> submissionFiles) {
    refreshFiles(transformFiles(submissionFiles));
  }

  public void refreshFiles(@NonNull Map<String, FileType> currentFiles) {
    val previousFiles = getFiles();

    // Calculate the difference between files:
    val diff = difference(previousFiles, currentFiles);
    val addedFiles = diff.entriesOnlyOnRight();
    val removedFiles = diff.entriesOnlyOnLeft();

    // Remove files that have been deleted
    for (val entry : addedFiles.entrySet()) {
      addFile(entry.getValue(), entry.getKey());
    }

    // Add files that have been submitted
    for (val entry : removedFiles.entrySet()) {
      removeFile(entry.getValue(), entry.getKey());
    }
  }

  public void addFile(@NonNull FileType fileType, @NonNull String fileName) {
    executeVisitor(new AddFileVisitor(fileName, fileType));
  }

  public void removeFile(@NonNull FileType fileType, @NonNull String fileName) {
    executeVisitor(new RemoveFileVisitor(fileName, fileType));
  }

  public Optional<FileReport> getFileReport(@NonNull String fileName) {
    return executeVisitor(new GetFileReportVisitor(fileName)).getFileReport();
  }

  public int getErrorCount() {
    return executeVisitor(new ErrorCountVisitor()).getErrorCount();
  }

  public boolean hasErrors() {
    return getErrorCount() > 0;
  }

  public boolean isValid() {
    return executeVisitor(new IsValidVisitor()).isValid();
  }

  public void resetAll() {
    resetDataTypes();
  }

  public void resetDataTypes(DataType... dataTypes) {
    resetDataTypes(copyOf(dataTypes));
  }

  public void resetDataTypes(@NonNull Iterable<DataType> dataTypes) {
    executeVisitor(new ResetVisitor(dataTypes));
  }

  public void inheritState(@NonNull SubmissionState state, @NonNull Iterable<DataType> dataTypes) {
    executeVisitor(new InheritStateVisitor(state, dataTypes));
  }

  public void abort(@NonNull Iterable<DataType> dataTypes) {
    executeVisitor(new AbortVisitor(dataTypes));
  }

  public void refreshState() {
    executeVisitor(new RefreshStateVisitor());
  }

  public void mergeReport(@NonNull Report originalReport, @NonNull Iterable<DataType> dataTypes) {
    new Merger().mergeInOriginalReport(originalReport, dataTypes);
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

  private class Merger {

    public void mergeInOriginalReport(@NonNull Report originalReport, @NonNull Iterable<DataType> selectedDataTypes) {
      for (val originalDataTypeReport : originalReport.getDataTypeReports()) {
        if (!isProcessedType(originalDataTypeReport, selectedDataTypes)) {
          log.info("Merging in original data type report for: '{}'", originalDataTypeReport.getDataType());
          replaceDataTypeReport(originalDataTypeReport);
        } else {
          log.info("Keeping new report for '{}'", originalDataTypeReport.getDataType());
        }
      }
    }

    private void replaceDataTypeReport(final DataTypeReport originalDataTypeReport) {
      Optional<DataTypeReport> optionalStaleDataTypeReport =
          Iterables.tryFind(dataTypeReports, new Predicate<DataTypeReport>() {

            @Override
            public boolean apply(DataTypeReport input) {
              return input.getDataType() == originalDataTypeReport.getDataType();
            }

          });
      if (optionalStaleDataTypeReport.isPresent()) {
        removeDataTypeReport(optionalStaleDataTypeReport.get());
      }
      addDataTypeReport(originalDataTypeReport);
    }

    private boolean isProcessedType(DataTypeReport dataTypeReport, @NonNull Iterable<DataType> selectedDataTypes) {
      return newArrayList(selectedDataTypes).contains(dataTypeReport.getDataType());
    }

  }

}
