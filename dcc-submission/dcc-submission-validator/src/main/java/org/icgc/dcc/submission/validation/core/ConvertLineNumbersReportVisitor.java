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
package org.icgc.dcc.submission.validation.core;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.core.report.ErrorReport;
import org.icgc.dcc.submission.core.report.FieldErrorReport;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.visitor.AbstractReportVisitor;
import org.icgc.dcc.submission.validation.primary.report.ByteOffsetToLineNumber;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * Needed for updating byte offsets to line numbers
 */
@RequiredArgsConstructor
public class ConvertLineNumbersReportVisitor extends AbstractReportVisitor {

  @NonNull
  private final Path filePath;

  @Override
  public void visit(FileReport fileReport) {
    for (val errorReport : fileReport.getErrorReports()) {
      if (!errorReport.isConverted()) {
        convert(errorReport);

        // Remember we converted so that we don't do again
        errorReport.setConverted(true);
      }
    }
  }

  private void convert(ErrorReport errorReport) {
    // Convert byte offsets to line numbers
    val mapping = translate(errorReport);

    // Update the report using the mapping
    update(errorReport, mapping);
  }

  private Map<Long, Long> translate(ErrorReport errorReport) {
    val offsets = ImmutableSet.<Long> builder();

    val fieldErrorReports = errorReport.getFieldErrorReports();
    for (val fieldErrorReport : fieldErrorReports) {
      offsets.addAll(fieldErrorReport.getLineNumbers());
    }

    return ByteOffsetToLineNumber.convert(filePath, offsets.build());
  }

  private void update(ErrorReport errorReport, Map<Long, Long> mapping) {
    for (val fieldErrorReport : errorReport.getFieldErrorReports()) {
      fieldErrorReport.setLineNumbers(map(mapping, fieldErrorReport));
    }
  }

  private List<Long> map(Map<Long, Long> mapping, FieldErrorReport fieldErrorReport) {
    val newLineNumbers = Lists.<Long> newLinkedList();
    for (val byteOffset : fieldErrorReport.getLineNumbers()) {
      newLineNumbers.add(mapping.get(byteOffset));
    }

    return newLineNumbers;
  }

}