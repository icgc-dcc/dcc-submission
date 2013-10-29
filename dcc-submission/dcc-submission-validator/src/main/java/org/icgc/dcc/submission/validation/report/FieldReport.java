/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.report;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

import org.icgc.dcc.submission.dictionary.model.SummaryType;
import org.icgc.dcc.submission.validation.report.BaseStatsReportingPlanElement.FieldSummary;

import com.google.code.morphia.annotations.Embedded;
import com.mongodb.BasicDBObject;

@Embedded
@Getter
@Setter
public class FieldReport implements Serializable {

  protected String name;
  protected long nulls;
  protected long missing;
  protected long populated;
  protected double completeness;
  protected BasicDBObject summary;

  protected String label;
  protected SummaryType type;

  public static FieldReport convert(FieldSummary fieldSummary) {
    val fieldReport = new FieldReport();
    fieldReport.setName(fieldSummary.field);
    fieldReport.setNulls(fieldSummary.nulls);
    fieldReport.setMissing(fieldSummary.missing);
    fieldReport.setPopulated(fieldSummary.populated);
    fieldReport.setCompleteness(calculateCompleteness(fieldSummary));
    fieldReport.setSummary(createSummary(fieldSummary));

    return fieldReport;
  }

  private static long calculateCompleteness(FieldSummary fieldSummary) {
    val available = fieldSummary.populated;
    val total = fieldSummary.nulls + fieldSummary.missing + available;

    return 100 * available / total;
  }

  private static BasicDBObject createSummary(FieldSummary fieldSummary) {
    val summary = new BasicDBObject();
    for (val key : fieldSummary.summary.keySet()) {
      summary.append(escape(key), fieldSummary.summary.get(key));
    }

    return summary;
  }

  private static String escape(String value) {
    // NOTE: periods and dollar signs must be replaced for MongoDB compatibility
    return value.replace(".", "&#46;").replace("$", "&#36;");
  }

}
