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

import static com.google.common.base.Optional.absent;
import static com.google.common.collect.Sets.newTreeSet;

import java.util.Collection;
import java.util.Set;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.model.SubmissionDataType;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.submission.core.model.SubmissionFile;
import org.mongodb.morphia.annotations.Embedded;

import com.google.common.base.Optional;

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
public class Report {

  @Getter(value = AccessLevel.PRIVATE)
  private Set<DataTypeReport> dataTypeReports = newTreeSet();

  public Report(@NonNull Iterable<SubmissionFile> submissionFiles) {
    for (val submissionFile : submissionFiles) {
      val fileName = submissionFile.getName();
      val fileType = SubmissionFileType.from(submissionFile.getDataType());
      val fileReport = new FileReport(fileName);

      addFileReport(fileType, fileReport);
    }
  }

  public Report(@NonNull Report report) {
    for (val dataTypeReport : report.getDataTypeReports()) {
      dataTypeReports.add(new DataTypeReport(dataTypeReport));
    }
  }

  public void reset() {
    for (val dataTypeReport : dataTypeReports) {
      dataTypeReport.reset();
    }
  }

  public void resetDataTypes(@NonNull Collection<SubmissionDataType> dataTypes) {
    for (val dataTypeReport : dataTypeReports) {
      val match = dataTypes.contains(dataTypeReport.getDataType());
      if (match) {
        dataTypeReport.reset();
      }
    }
  }

  public Optional<DataTypeReport> getDataTypeReport(@NonNull SubmissionDataType dataType) {
    for (val dataTypeReport : dataTypeReports) {
      if (dataType == dataTypeReport.getDataType()) {
        return Optional.of(dataTypeReport);
      }
    }

    return absent();
  }

  public Optional<FileTypeReport> getFileTypeReport(@NonNull SubmissionFileType fileType) {
    for (val dataTypeReport : dataTypeReports) {
      val optional = dataTypeReport.getFileTypeReport(fileType);
      if (optional.isPresent()) {
        return optional;
      }
    }

    return absent();
  }

  public Optional<FileReport> getFileReport(@NonNull String fileName) {
    for (val dataTypeReport : dataTypeReports) {
      val optional = dataTypeReport.getFileReport(fileName);
      if (optional.isPresent()) {
        return optional;
      }
    }

    return absent();
  }

  public void addFileReport(@NonNull SubmissionFileType fileType, @NonNull FileReport fileReport) {
    val dataType = fileType.getDataType();
    val optional = getDataTypeReport(dataType);
    val dataTypeReport = optional.isPresent() ? optional.get() : new DataTypeReport(dataType);

    dataTypeReport.addFileReport(fileType, fileReport);
  }

  public void removeFileReport(@NonNull SubmissionFileType fileType, @NonNull FileReport fileReport) {
    val dataType = fileType.getDataType();
    val optional = getDataTypeReport(dataType);
    if (optional.isPresent()) {
      val dataTypeReport = optional.get();

      dataTypeReport.removeFileReport(fileType, fileReport);
      if (dataTypeReport.getFileTypeReports().isEmpty()) {
        dataTypeReports.remove(dataTypeReport);
      }
    }
  }

}
