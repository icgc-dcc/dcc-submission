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

import java.util.Collection;

import javax.annotation.concurrent.NotThreadSafe;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.core.model.SubmissionDataType;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.submission.core.report.DataTypeReport;
import org.icgc.dcc.submission.core.report.DataTypeState;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.FileState;
import org.icgc.dcc.submission.core.report.FileTypeReport;
import org.icgc.dcc.submission.core.report.FileTypeState;

@NotThreadSafe
@RequiredArgsConstructor
public class ResetReportVisitor extends AbstractReportVisitor {

  @NonNull
  private final Collection<SubmissionDataType> dataTypes;

  public ResetReportVisitor() {
    this.dataTypes = newHashSet();
  }

  @Override
  public void visit(@NonNull DataTypeReport dataTypeReport) {
    if (isResettable(dataTypeReport.getDataType())) {
      dataTypeReport.setDataTypeState(DataTypeState.getDefaultState());
    }
  }

  @Override
  public void visit(@NonNull FileTypeReport fileTypeReport) {
    if (isResettable(fileTypeReport.getFileType())) {
      fileTypeReport.setFileTypeState(FileTypeState.getDefaultState());
    }
  }

  @Override
  public void visit(@NonNull FileReport fileReport) {
    if (isResettable(fileReport.getFileType())) {
      fileReport.setFileState(FileState.getDefaultState());

      // Clear all leaf level reports
      fileReport.getSummaryReports().clear();
      fileReport.getFieldReports().clear();
      fileReport.getErrorReports().clear();
    }
  }

  private boolean isResettable(SubmissionDataType dataType) {
    return dataTypes.isEmpty() || dataTypes.contains(dataType);
  }

  private boolean isResettable(SubmissionFileType fileType) {
    return isResettable(fileType.getDataType());
  }

}
