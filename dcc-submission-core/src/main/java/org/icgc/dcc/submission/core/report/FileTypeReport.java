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
import static org.icgc.dcc.submission.core.report.FileTypeState.getDefaultState;

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
 * Reports on a {@link FileType}.
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
@EqualsAndHashCode(of = "fileType")
public class FileTypeReport implements ReportElement, Comparable<FileTypeReport> {

  /**
   * Key.
   */
  @JsonSerialize(using = FileTypeSerializer.class)
  @JsonDeserialize(using = FileTypeDeserializer.class)
  private FileType fileType;

  private FileTypeState fileTypeState = getDefaultState();
  private Set<FileReport> fileReports = newTreeSet();

  public FileTypeReport(@NonNull FileType fileType) {
    this.fileType = fileType;
  }

  public FileTypeReport(@NonNull FileTypeReport fileTypeReport) {
    this.fileType = fileTypeReport.fileType;
    this.fileTypeState = fileTypeReport.fileTypeState;

    for (val fileReport : fileTypeReport.fileReports) {
      fileReports.add(new FileReport(fileReport));
    }
  }

  @Override
  public void accept(@NonNull ReportVisitor visitor) {
    // Children first
    for (val fileReport : fileReports) {
      fileReport.accept(visitor);
    }

    // Self last
    visitor.visit(this);
  }

  public void addFileReport(@NonNull FileReport fileReport) {
    fileReports.add(fileReport);
  }

  public void removeFileReport(@NonNull FileReport fileReport) {
    fileReports.remove(fileReport);
  }

  @Override
  public int compareTo(@NonNull FileTypeReport other) {
    return fileType.compareTo(other.fileType);
  }

}
