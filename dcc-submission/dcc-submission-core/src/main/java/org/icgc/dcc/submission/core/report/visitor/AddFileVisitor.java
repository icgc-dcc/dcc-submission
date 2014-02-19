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
package org.icgc.dcc.submission.core.report.visitor;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.report.DataTypeReport;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.FileTypeReport;
import org.icgc.dcc.submission.core.report.Report;

public class AddFileVisitor extends AbstractFileReportVisitor {

  public AddFileVisitor(@NonNull String fileName, @NonNull FileType fileType) {
    super(fileName, fileType);
  }

  @Override
  public void visit(@NonNull Report report) {
    if (isAddable(dataTypeReport)) {
      report.addDataTypeReport(createDataTypeReport());
    }
  }

  @Override
  public void visit(@NonNull DataTypeReport dataTypeReport) {
    if (isTarget(dataTypeReport) && isAddable(fileTypeReport)) {
      this.dataTypeReport = dataTypeReport;
      this.dataTypeReport.addFileTypeReport(createFileTypeReport());
    }
  }

  @Override
  public void visit(@NonNull FileTypeReport fileTypeReport) {
    if (isTarget(fileTypeReport) && isAddable(fileReport)) {
      this.fileTypeReport = fileTypeReport;
      this.fileTypeReport.addFileReport(createFileReport());
    }
  }

  @Override
  public void visit(@NonNull FileReport fileReport) {
    if (isTarget(fileReport)) {
      this.fileReport = fileReport;
    }
  }

  private static boolean isAddable(DataTypeReport dataTypeReport) {
    return dataTypeReport == null;
  }

  private static boolean isAddable(FileTypeReport fileTypeReport) {
    return fileTypeReport == null;
  }

  private static boolean isAddable(FileReport fileReport) {
    return fileReport == null;
  }

  private DataTypeReport createDataTypeReport() {
    val dataTypeReport = new DataTypeReport(fileType.getDataType());
    dataTypeReport.addFileTypeReport(createFileTypeReport());

    return dataTypeReport;
  }

  private FileTypeReport createFileTypeReport() {
    val fileTypeReport = new FileTypeReport(fileType);
    fileTypeReport.addFileReport(createFileReport());

    return fileTypeReport;
  }

  private FileReport createFileReport() {
    return new FileReport(fileName, fileType);
  }

}
