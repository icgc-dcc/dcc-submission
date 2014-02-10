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
  private final Set<DataType> invalidDataTypes = newHashSet();
  private final Set<FileType> validFileTypes = newHashSet();
  private final Set<FileType> invalidFileTypes = newHashSet();

  @Override
  public void visit(DataTypeReport dataTypeReport) {
    if (isValid(dataTypeReport) && hasErrors(dataTypeReport)) {
      dataTypeReport.setDataTypeState(DataTypeState.INVALID);
    }

    if (isInvalid(dataTypeReport) && hasNoErrors(dataTypeReport)) {
      dataTypeReport.setDataTypeState(DataTypeState.VALID);
    }
  }

  @Override
  public void visit(FileTypeReport fileTypeReport) {
    if (isValid(fileTypeReport) && hasErrors(fileTypeReport)) {
      fileTypeReport.setFileTypeState(FileTypeState.INVALID);
    }

    if (isInvalid(fileTypeReport) && hasNoErrors(fileTypeReport)) {
      fileTypeReport.setFileTypeState(FileTypeState.VALID);
    }
  }

  @Override
  public void visit(FileReport fileReport) {
    if (isFileStateIn(fileReport, FileState.VALID, FileState.NOT_VALIDATED) && hasErrors(fileReport)) {
      fileReport.setFileState(FileState.INVALID);

      markInvalid(fileReport);
    }

    if (isFileStateIn(fileReport, FileState.INVALID, FileState.NOT_VALIDATED) && hasErrors(fileReport)) {
      fileReport.setFileState(FileState.VALID);

      markValid(fileReport);
    }
  }

  private boolean isValid(DataTypeReport dataTypeReport) {
    return dataTypeReport.getDataTypeState() == DataTypeState.VALID;
  }

  private boolean isInvalid(DataTypeReport dataTypeReport) {
    return dataTypeReport.getDataTypeState() == DataTypeState.INVALID;
  }

  private boolean hasErrors(DataTypeReport dataTypeReport) {
    return invalidFileTypes.contains(dataTypeReport.getDataType());
  }

  private boolean hasNoErrors(DataTypeReport dataTypeReport) {
    return validFileTypes.contains(dataTypeReport.getDataType());
  }

  private boolean isValid(FileTypeReport fileTypeReport) {
    return fileTypeReport.getFileTypeState() == FileTypeState.VALID;
  }

  private boolean isInvalid(FileTypeReport fileTypeReport) {
    return fileTypeReport.getFileTypeState() == FileTypeState.INVALID;
  }

  private boolean hasErrors(FileTypeReport fileTypeReport) {
    return invalidFileTypes.contains(fileTypeReport.getFileType());
  }

  private boolean hasNoErrors(FileTypeReport fileTypeReport) {
    return validFileTypes.contains(fileTypeReport.getFileType());
  }

  private boolean isFileStateIn(FileReport fileReport, FileState... fileStates) {
    for (val fileState : fileStates) {
      if (fileReport.getFileState() == fileState) {
        return true;
      }
    }

    return false;
  }

  private boolean hasErrors(FileReport fileReport) {
    return !hasNoErrors(fileReport);
  }

  private boolean hasNoErrors(FileReport fileReport) {
    return fileReport.getErrorReports().isEmpty();
  }

  private void markValid(FileReport fileReport) {
    validFileTypes.add(fileReport.getFileType());
    validDataTypes.add(fileReport.getFileType().getDataType());
  }

  private void markInvalid(FileReport fileReport) {
    invalidFileTypes.add(fileReport.getFileType());
    invalidDataTypes.add(fileReport.getFileType().getDataType());
  }

}
