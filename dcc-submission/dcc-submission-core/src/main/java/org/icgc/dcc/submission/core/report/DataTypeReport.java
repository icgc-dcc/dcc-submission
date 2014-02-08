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

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.model.SubmissionDataType;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.mongodb.morphia.annotations.Embedded;

import com.google.common.base.Optional;

@Data
@Embedded
@NoArgsConstructor
@AllArgsConstructor
public class DataTypeReport implements Comparable<DataTypeReport> {

  private SubmissionDataType dataType;
  private DataTypeState dataTypeState = DataTypeState.NOT_VALIDATED;
  private Set<FileTypeReport> fileTypeReports = newTreeSet();

  public DataTypeReport(@NonNull SubmissionDataType dataType) {
    this.dataType = dataType;
  }

  public Optional<FileTypeReport> getFileTypeReport(@NonNull SubmissionFileType fileType) {
    for (val fileTypeReport : fileTypeReports) {
      if (fileType == fileTypeReport.getFileType()) {
        return Optional.of(fileTypeReport);
      }
    }

    return absent();
  }

  public Optional<FileReport> getFileReport(@NonNull String fileName) {
    for (val fileTypeReport : fileTypeReports) {
      val optional = fileTypeReport.getFileReport(fileName);
      if (optional.isPresent()) {
        return optional;
      }
    }

    return absent();
  }

  public void addFileReport(@NonNull SubmissionFileType fileType, @NonNull FileReport fileReport) {
    val optional = getFileTypeReport(fileType);
    val fileTypeReport = optional.isPresent() ? optional.get() : new FileTypeReport(fileType);

    fileTypeReport.addFileReport(fileReport);
  }

  public void removeFileReport(@NonNull SubmissionFileType fileType, @NonNull FileReport fileReport) {
    val optional = getFileTypeReport(fileType);
    if (optional.isPresent()) {
      val fileTypeReport = optional.get();

      fileTypeReport.removeFileReport(fileReport);
      if (fileTypeReport.getFileReports().isEmpty()) {
        fileTypeReports.remove(fileTypeReport);
      }
    }
  }

  @Override
  public int compareTo(@NonNull DataTypeReport other) {
    return dataType.toString().compareTo(other.toString());
  }

}
