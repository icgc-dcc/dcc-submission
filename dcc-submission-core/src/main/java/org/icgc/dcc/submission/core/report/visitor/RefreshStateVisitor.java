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

import static com.google.common.collect.Sets.newHashSet;

import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.report.DataTypeReport;
import org.icgc.dcc.submission.core.report.DataTypeState;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.FileState;
import org.icgc.dcc.submission.core.report.FileTypeReport;
import org.icgc.dcc.submission.core.report.FileTypeState;
import org.icgc.dcc.submission.core.report.ReportElement;

/**
 * Refreshes stale {@link ReportElement} states based on non-state attributes and transitive relationships.
 */
@RequiredArgsConstructor
public class RefreshStateVisitor extends NoOpVisitor {

  /**
   * State - journals the <em>valid<em> file and data types as visiting proceeds.
   */
  private final Set<DataType> validDataTypes = newHashSet();
  private final Set<FileType> validFileTypes = newHashSet();

  /**
   * State - journals the <em>invalid</em> file and data types as visiting proceeds.
   */
  private final Set<DataType> invalidDataTypes = newHashSet();
  private final Set<FileType> invalidFileTypes = newHashSet();

  /**
   * Refreshes any dirty valid or invalid states at the data-type level.
   */
  @Override
  public void visit(DataTypeReport dataTypeReport) {
    // Use the file type results
    if (refreshInvalid(dataTypeReport)) {
      dataTypeReport.setDataTypeState(DataTypeState.INVALID);
    } else if (refreshValid(dataTypeReport)) {
      dataTypeReport.setDataTypeState(DataTypeState.VALID);
    }
  }

  /**
   * Refreshes any dirty valid or invalid states at the file-type level.
   */
  @Override
  public void visit(FileTypeReport fileTypeReport) {
    // Use the file type results
    if (refreshInvalid(fileTypeReport)) {
      fileTypeReport.setFileTypeState(FileTypeState.INVALID);
    } else if (refreshValid(fileTypeReport)) {
      fileTypeReport.setFileTypeState(FileTypeState.VALID);
    }
  }

  /**
   * Refreshes any dirty valid or invalid states at the file level.
   * <p>
   * Tracks valid and invalid types for higher levels.
   */
  @Override
  public void visit(FileReport fileReport) {
    if (refreshInvalid(fileReport)) {
      // Errors:
      fileReport.setFileState(FileState.INVALID);

      // Track
      recordInvalid(fileReport);
    } else if (refreshValid(fileReport)) {
      // Clean:
      fileReport.setFileState(FileState.VALID);

      // Track
      recordValid(fileReport);
    } else if (fileReport.getFileState().in(FileState.VALID)) {
      // Pass-through:
      // - Was previously validated

      // Track
      recordValid(fileReport);
    } else if (fileReport.getFileState().in(FileState.INVALID)) {
      // Pass-through:
      // - Was previously validated

      // Track
      recordInvalid(fileReport);
    }
  }

  //
  // Helpers - Refresh valid predicates
  //

  private boolean refreshValid(DataTypeReport dataTypeReport) {
    return dataTypeReport.getDataTypeState().in(DataTypeState.INVALID, DataTypeState.VALIDATING)
        && isValid(dataTypeReport.getDataType());
  }

  private boolean refreshValid(FileTypeReport fileTypeReport) {
    return fileTypeReport.getFileTypeState().in(FileTypeState.INVALID, FileTypeState.VALIDATING)
        && isValid(fileTypeReport.getFileType());
  }

  private boolean refreshValid(FileReport fileReport) {
    return fileReport.getFileState().in(FileState.INVALID, FileState.VALIDATING)
        && isValid(fileReport);
  }

  //
  // Helpers - Refresh invalid predicates
  //

  private boolean refreshInvalid(DataTypeReport dataTypeReport) {
    return dataTypeReport.getDataTypeState().in(DataTypeState.VALID, DataTypeState.VALIDATING)
        && isInvalid(dataTypeReport.getDataType());
  }

  private boolean refreshInvalid(FileTypeReport fileTypeReport) {
    return fileTypeReport.getFileTypeState().in(FileTypeState.VALID, FileTypeState.VALIDATING)
        && isInvalid(fileTypeReport.getFileType());
  }

  private boolean refreshInvalid(FileReport fileReport) {
    return fileReport.getFileState().in(FileState.VALID, FileState.VALIDATING)
        && isInvalid(fileReport);
  }

  //
  // Helpers - valid predicates
  //

  private boolean isValid(DataType dataType) {
    return validDataTypes.contains(dataType) && !invalidDataTypes.contains(dataType);
  }

  private boolean isValid(FileType fileType) {
    return validFileTypes.contains(fileType) && !invalidFileTypes.contains(fileType);
  }

  private boolean isValid(FileReport fileReport) {
    return fileReport.getErrorReports().isEmpty();
  }

  //
  // Helpers - invalid predicates
  //

  private boolean isInvalid(DataType dataType) {
    return invalidDataTypes.contains(dataType);
  }

  private boolean isInvalid(FileType fileType) {
    return invalidFileTypes.contains(fileType);
  }

  private boolean isInvalid(FileReport fileReport) {
    return !isValid(fileReport);
  }

  //
  // Helpers - journaling
  //

  private void recordValid(FileReport fileReport) {
    validFileTypes.add(fileReport.getFileType());
    validDataTypes.add(fileReport.getFileType().getDataType());
  }

  private void recordInvalid(FileReport fileReport) {
    invalidFileTypes.add(fileReport.getFileType());
    invalidDataTypes.add(fileReport.getFileType().getDataType());
  }

}
