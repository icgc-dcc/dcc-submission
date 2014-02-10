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

import lombok.RequiredArgsConstructor;
import lombok.val;

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.report.DataTypeReport;
import org.icgc.dcc.submission.core.report.DataTypeState;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.FileState;
import org.icgc.dcc.submission.core.report.FileTypeReport;
import org.icgc.dcc.submission.core.report.FileTypeState;

@RequiredArgsConstructor
public class RefreshStateReportVisitor extends AbstractReportVisitor {

  /**
   * Accumulation
   */
  private final Set<DataType> validDataTypes = newHashSet();
  private final Set<FileType> validFileTypes = newHashSet();
  private final Set<DataType> invalidDataTypes = newHashSet();
  private final Set<FileType> invalidFileTypes = newHashSet();

  @Override
  public void visit(DataTypeReport dataTypeReport) {
    if (isValid(dataTypeReport)) {
      dataTypeReport.setDataTypeState(DataTypeState.INVALID);
    } else if (isInvalid(dataTypeReport)) {
      dataTypeReport.setDataTypeState(DataTypeState.VALID);
    }
  }

  @Override
  public void visit(FileTypeReport fileTypeReport) {
    if (isValid(fileTypeReport)) {
      fileTypeReport.setFileTypeState(FileTypeState.INVALID);
    } else if (isInvalid(fileTypeReport)) {
      fileTypeReport.setFileTypeState(FileTypeState.VALID);
    }
  }

  @Override
  public void visit(FileReport fileReport) {
    if (isValid(fileReport)) {
      fileReport.setFileState(FileState.INVALID);

      recordInvalidFile(fileReport);
    } else if (isInvalid(fileReport)) {
      fileReport.setFileState(FileState.VALID);

      recordValidFile(fileReport);
    }
  }

  private boolean isValid(DataTypeReport dataTypeReport) {
    return dataTypeReport.getDataTypeState() == DataTypeState.VALID && hasErrors(dataTypeReport);
  }

  private boolean isValid(FileTypeReport fileTypeReport) {
    return fileTypeReport.getFileTypeState() == FileTypeState.VALID && hasErrors(fileTypeReport);
  }

  private boolean isValid(FileReport fileReport) {
    return isFileStateIn(fileReport, FileState.VALIDATING) && hasErrors(fileReport);
  }

  private boolean isInvalid(DataTypeReport dataTypeReport) {
    return dataTypeReport.getDataTypeState() == DataTypeState.INVALID && hasNoErrors(dataTypeReport);
  }

  private boolean isInvalid(FileTypeReport fileTypeReport) {
    return fileTypeReport.getFileTypeState() == FileTypeState.INVALID && hasNoErrors(fileTypeReport);
  }

  private boolean isInvalid(FileReport fileReport) {
    return isFileStateIn(fileReport, FileState.VALIDATING) && hasNoErrors(fileReport);
  }

  private boolean hasNoErrors(DataTypeReport dataTypeReport) {
    val dataType = dataTypeReport.getDataType();
    return validDataTypes.contains(dataTypeReport.getDataType()) && !validDataTypes.contains(dataType);
  }

  private boolean hasNoErrors(FileTypeReport fileTypeReport) {
    val fileType = fileTypeReport.getFileType();
    return validFileTypes.contains(fileTypeReport.getFileType()) && !invalidFileTypes.contains(fileType);
  }

  private boolean hasNoErrors(FileReport fileReport) {
    return fileReport.getErrorReports().isEmpty();
  }

  private boolean hasErrors(DataTypeReport dataTypeReport) {
    val dataType = dataTypeReport.getDataType();
    return invalidDataTypes.contains(dataType);
  }

  private boolean hasErrors(FileTypeReport fileTypeReport) {
    val fileType = fileTypeReport.getFileType();
    return invalidFileTypes.contains(fileType);
  }

  private boolean hasErrors(FileReport fileReport) {
    return !hasNoErrors(fileReport);
  }

  private boolean isFileStateIn(FileReport fileReport, FileState... fileStates) {
    for (val fileState : fileStates) {
      if (fileReport.getFileState() == fileState) {
        return true;
      }
    }

    return false;
  }

  private void recordValidFile(FileReport fileReport) {
    validFileTypes.add(fileReport.getFileType());
    validDataTypes.add(fileReport.getFileType().getDataType());
  }

  private void recordInvalidFile(FileReport fileReport) {
    invalidFileTypes.add(fileReport.getFileType());
    invalidDataTypes.add(fileReport.getFileType().getDataType());
  }

}
