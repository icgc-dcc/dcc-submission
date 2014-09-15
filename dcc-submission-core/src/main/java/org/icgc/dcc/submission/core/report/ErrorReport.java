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
package org.icgc.dcc.submission.core.report;

import static com.google.common.collect.ComparisonChain.start;

import java.util.Set;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.mongodb.morphia.annotations.Embedded;

import com.google.common.collect.Sets;

/**
 * Reports on a file validation error.
 * <p>
 * Example:
 * 
 * <pre>
 *  {
 *    "errorType": "STRUCTURALLY_INVALID_ROW_ERROR",
 *    "number": "0",
 *    "description": "This is a description",
 *    "fieldErrorReports": [ {
 *      "fieldNames": [ "f1" ],
 *      "parameters" : {
 *        ...
 *      },
 *      "count": "10,
 *      "lineNumbers": [ 10, 20, 30 ],
 *      "value": [ "v1", "v2", "v3" ]
 *    } ]
 *  }
 * </pre>
 */
@Data
@Embedded
@NoArgsConstructor
@EqualsAndHashCode(of = { "errorType", "number" })
public class ErrorReport implements ReportElement, Comparable<ErrorReport> {

  /**
   * Key.
   */
  private ErrorType errorType;
  private int number; // FIXME: https://jira.oicr.on.ca/browse/DCC-2087

  private String description;

  private final Set<FieldErrorReport> fieldErrorReports = Sets.newLinkedHashSet();

  /**
   * Temporary band-aid to fix the issue of bite offsets being converted twice (see DCC-1908).
   */
  private boolean converted = false;

  public ErrorReport(@NonNull ErrorType errorType, int number, String description) {
    this.errorType = errorType;
    this.number = number;
    this.description = description;
  }

  public ErrorReport(@NonNull ErrorReport errorReport) {
    this.errorType = errorReport.errorType;
    this.number = errorReport.number;

    this.description = errorReport.description;

    for (val fieldErrorReport : errorReport.fieldErrorReports) {
      fieldErrorReports.add(new FieldErrorReport(fieldErrorReport));
    }
  }

  @Override
  public void accept(@NonNull ReportVisitor visitor) {
    visitor.visit(this);
  }

  public void addError(@NonNull Error error) {
    val fieldErrorReport = resolveFieldErrorReport(error);
    fieldErrorReport.addError(error);
  }

  public boolean reportsOn(@NonNull Error error) {
    return errorType == error.getType() && number == error.getNumber();
  }

  @Override
  public int compareTo(@NonNull ErrorReport other) {
    return start()
        .compare(this.errorType, other.errorType)
        .compare(this.number, other.number)
        .result();
  }

  private FieldErrorReport resolveFieldErrorReport(Error error) {
    FieldErrorReport fieldErrorReport = getFieldErrorReport(error);
    if (fieldErrorReport == null) {
      fieldErrorReport = new FieldErrorReport(error.getFieldNames(), error.getType().build(error.getParams()));

      fieldErrorReports.add(fieldErrorReport);
    }

    return fieldErrorReport;
  }

  private FieldErrorReport getFieldErrorReport(@NonNull Error error) {
    for (val fieldErrorReport : fieldErrorReports) {
      if (fieldErrorReport.reportsOn(error)) {
        return fieldErrorReport;
      }
    }

    return null;
  }

}
