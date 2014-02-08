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
import static org.icgc.dcc.submission.core.report.FileTypeState.getDefaultState;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.mongodb.morphia.annotations.Embedded;

import com.google.common.base.Optional;

/**
 * Reports on a {@link SubmissionFileType}.
 * <p>
 * Example:
 * 
 * <pre>
 *  {
 *    "fileType": "DONOR",
 *    "FileTypeState": "NOT_VALIDATED",
 *    "fileReports": [ {
 *      ...
 *    } ]
 *  }
 * </pre>
 */
@Data
@Embedded
@NoArgsConstructor
@AllArgsConstructor
public class FileTypeReport implements Comparable<FileTypeReport> {

  private SubmissionFileType fileType;
  private FileTypeState fileTypeState = getDefaultState();
  private Set<FileReport> fileReports = newTreeSet();

  public FileTypeReport(@NonNull FileTypeReport fileTypeReport) {
    this.fileType = fileTypeReport.fileType;
    this.fileTypeState = fileTypeReport.fileTypeState;

    for (val fileReport : fileTypeReport.fileReports) {
      fileReports.add(new FileReport(fileReport));
    }
  }

  public FileTypeReport(@NonNull SubmissionFileType fileType) {
    this.fileType = fileType;
  }

  public void reset() {
    fileTypeState = getDefaultState();

    for (val fileReport : fileReports) {
      fileReport.reset();
    }
  }

  public Optional<FileReport> getFileReport(@NonNull String fileName) {
    for (val fileReport : fileReports) {
      if (fileReport.getFileName().equals(fileName)) {
        return Optional.of(fileReport);
      }
    }

    return absent();
  }

  public void addFileReport(@NonNull FileReport fileReport) {
    fileReports.add(fileReport);
  }

  public void removeFileReport(@NonNull FileReport fileReport) {
    val optional = getFileReport(fileReport.getFileName());
    if (optional.isPresent()) {
      fileReports.remove(optional.get());
    }
  }

  @Override
  public int compareTo(@NonNull FileTypeReport other) {
    return fileType.compareTo(other.fileType);
  }

}
