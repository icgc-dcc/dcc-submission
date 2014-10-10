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

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

import lombok.NonNull;

import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.report.DataTypeReport;
import org.icgc.dcc.submission.core.report.DataTypeState;
import org.icgc.dcc.submission.core.report.Error;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.FileState;
import org.icgc.dcc.submission.core.report.FileTypeReport;
import org.icgc.dcc.submission.core.report.FileTypeState;

public class AddErrorVisitor extends AbstractFileNameReportVisitor {

  /**
   * Input
   */
  private final Error error;

  /**
   * Accumulation
   */
  private final Set<DataType> dataTypes = newHashSet();
  private final Set<FileType> fileTypes = newHashSet();

  @SuppressWarnings("unused")
  public AddErrorVisitor(@NonNull Error error) {
    super(error.getFileName());
    this.error = error;
  }

  //
  // Data Type
  //

  @Override
  public void visit(DataTypeReport dataTypeReport) {
    if (isTarget(dataTypeReport)) {
      dataTypeReport.setDataTypeState(DataTypeState.INVALID);
    }
  }

  //
  // File Type
  //

  @Override
  public void visit(FileTypeReport fileTypeReport) {
    if (isTarget(fileTypeReport)) {
      fileTypeReport.setFileTypeState(FileTypeState.INVALID);
    }
  }

  //
  // File
  //

  @Override
  public void visit(@NonNull FileReport fileReport) {
    if (isTarget(fileReport)) {
      fileReport.setFileState(FileState.INVALID);
      fileReport.addError(error);

      // For ancestors
      fileTypes.add(fileReport.getFileType());
      dataTypes.add(fileReport.getFileType().getDataType());
    }
  }

  //
  // Helpers
  //

  private boolean isTarget(DataTypeReport dataTypeReport) {
    return dataTypes.contains(dataTypeReport.getDataType());
  }

  private boolean isTarget(FileTypeReport fileTypeReport) {
    return fileTypes.contains(fileTypeReport.getFileType());
  }

}
