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
package org.icgc.dcc.submission.validation.core;

import static com.google.common.base.Optional.fromNullable;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.core.report.ErrorReport;
import org.icgc.dcc.submission.core.report.FieldErrorReport;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.visitor.NoOpVisitor;
import org.icgc.dcc.submission.validation.primary.report.ByteOffsetToLineNumber;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * Needed for updating byte offsets to line numbers
 */
@RequiredArgsConstructor
public class ConvertLineNumbersReportVisitor extends NoOpVisitor {

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
    val mapping = createMapping(errorReport);

    // Is mapping needed?
    val needed = mapping.isPresent();
    if (needed) {
      // Update the report using the mapping
      updateErrorReport(errorReport, mapping.get());
    }
  }

  private Optional<Map<Long, Long>> createMapping(ErrorReport errorReport) {
    val offsets = ImmutableSet.<Long> builder();

    val fieldErrorReports = errorReport.getFieldErrorReports();
    for (val fieldErrorReport : fieldErrorReports) {
      offsets.addAll(fieldErrorReport.getLineNumbers());
    }

    val mapping = ByteOffsetToLineNumber.convert(filePath, offsets.build());
    return fromNullable(mapping);
  }

  private void updateErrorReport(ErrorReport errorReport, Map<Long, Long> mapping) {
    for (val fieldErrorReport : errorReport.getFieldErrorReports()) {
      fieldErrorReport.setLineNumbers(mapFieldErrorReport(fieldErrorReport, mapping));
    }
  }

  private List<Long> mapFieldErrorReport(FieldErrorReport fieldErrorReport, Map<Long, Long> mapping) {
    val lineNumbers = Lists.<Long> newLinkedList();
    for (val byteOffset : fieldErrorReport.getLineNumbers()) {
      val lineNumber = mapping.get(byteOffset);

      lineNumbers.add(lineNumber);
    }

    return lineNumbers;
  }

}