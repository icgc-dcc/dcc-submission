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

import java.util.Set;

import lombok.val;

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.report.DataTypeReport;
import org.icgc.dcc.submission.core.report.DataTypeState;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.FileState;
import org.icgc.dcc.submission.core.report.FileTypeReport;
import org.icgc.dcc.submission.core.report.FileTypeState;
import org.icgc.dcc.submission.core.report.ReportElement;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;

/**
 * Refreshes stale {@link ReportElement} states based on non-state attributes and transitive relationships.
 */
public abstract class UpdateStateVisitor extends NoOpVisitor {

  private final SetMultimap<FileType, FileState> fileTypeStates = createSetMultimap();

  @Override
  public void visit(DataTypeReport dataTypeReport) {
    val dataType = dataTypeReport.getDataType();
    val fileStates = getFileStates(dataType);

    if (fileStates.contains(FileState.ERROR)) {
      dataTypeReport.setDataTypeState(DataTypeState.ERROR);
      return;
    }

    if (fileStates.contains(FileState.INVALID)) {
      dataTypeReport.setDataTypeState(DataTypeState.INVALID);
      return;
    }

    if (fileStates.contains(FileState.NOT_VALIDATED)) {
      dataTypeReport.setDataTypeState(DataTypeState.NOT_VALIDATED);
      return;
    }

    if (fileStates.contains(FileState.VALID)) {
      dataTypeReport.setDataTypeState(DataTypeState.VALID);
      return;
    }
  }

  @Override
  public void visit(FileTypeReport fileTypeReport) {
    val fileType = fileTypeReport.getFileType();
    val fileStates = getFileStates(fileType);

    if (fileStates.contains(FileState.ERROR)) {
      fileTypeReport.setFileTypeState(FileTypeState.ERROR);
      return;
    }

    if (fileStates.contains(FileState.INVALID)) {
      fileTypeReport.setFileTypeState(FileTypeState.INVALID);
      return;
    }

    if (fileStates.contains(FileState.NOT_VALIDATED)) {
      fileTypeReport.setFileTypeState(FileTypeState.NOT_VALIDATED);
      return;
    }

    if (fileStates.contains(FileState.VALID)) {
      fileTypeReport.setFileTypeState(FileTypeState.VALID);
      return;
    }

  }

  @Override
  public void visit(FileReport fileReport) {
    val fileType = fileReport.getFileType();
    val fileState = fileReport.getFileState();

    fileTypeStates.put(fileType, fileState);
  }

  private Set<FileState> getFileStates(FileType fileType) {
    return fileTypeStates.get(fileType);
  }

  private Set<FileState> getFileStates(DataType dataType) {
    val fileStates = ImmutableSet.<FileState> builder();

    for (val fileType : getFileTypes(dataType)) {
      fileStates.addAll(getFileStates(fileType));
    }

    return fileStates.build();
  }

  private static Set<FileType> getFileTypes(DataType dataType) {
    val fileTypes = ImmutableSet.<FileType> builder();

    for (val fileType : FileType.values()) {
      if (fileType.getDataType() == dataType) {
        fileTypes.add(fileType);
      }
    }

    return fileTypes.build();
  }

  private static SetMultimap<FileType, FileState> createSetMultimap() {
    return HashMultimap.<FileType, FileState> create();
  }

}
