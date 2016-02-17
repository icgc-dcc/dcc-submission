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
package org.icgc.dcc.submission.core.report.visitor;

import javax.annotation.concurrent.NotThreadSafe;

import lombok.NonNull;

import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.report.DataTypeReport;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.FileTypeReport;
import org.icgc.dcc.submission.core.report.Report;

/**
 * Removes a file from a report and adjusts the internal structure to accommodate the loss.
 */
@NotThreadSafe
public class RemoveFileVisitor extends AbstractFileReportVisitor {

  public RemoveFileVisitor(@NonNull String fileName, @NonNull FileType fileType) {
    super(fileName, fileType);
  }

  @Override
  public void visit(@NonNull Report report) {
    if (isRemovable(dataTypeReport)) {
      report.removeDataTypeReport(dataTypeReport);
    }
  }

  @Override
  public void visit(@NonNull DataTypeReport dataTypeReport) {
    if (isTarget(dataTypeReport) && isRemovable(fileTypeReport)) {
      this.dataTypeReport = dataTypeReport;
      dataTypeReport.removeFileTypeReport(fileTypeReport);
    }
  }

  @Override
  public void visit(@NonNull FileTypeReport fileTypeReport) {
    if (isTarget(fileTypeReport) && isRemovable(fileReport)) {
      this.fileTypeReport = fileTypeReport;
      fileTypeReport.removeFileReport(fileReport);
    }
  }

  @Override
  public void visit(@NonNull FileReport fileReport) {
    if (isTarget(fileReport)) {
      this.fileReport = fileReport;
    }
  }

  //
  // Helpers
  //

  private static boolean isRemovable(DataTypeReport dataTypeReport) {
    return dataTypeReport != null && dataTypeReport.getFileTypeReports().isEmpty();
  }

  private static boolean isRemovable(FileTypeReport fileTypeReport) {
    return fileTypeReport != null && fileTypeReport.getFileReports().isEmpty();
  }

  private static boolean isRemovable(FileReport fileReport) {
    return fileReport != null;
  }

}
