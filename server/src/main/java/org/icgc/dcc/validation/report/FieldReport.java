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
package org.icgc.dcc.validation.report;

import java.io.Serializable;

import org.icgc.dcc.dictionary.model.SummaryType;
import org.icgc.dcc.validation.report.BaseStatsReportingPlanElement.FieldSummary;

import com.google.code.morphia.annotations.Embedded;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@Embedded
public class FieldReport implements Serializable {

  protected String name;

  protected double completeness;

  protected long nulls;

  protected long missing;

  protected long populated;

  protected BasicDBObject summary;

  protected String label;

  protected SummaryType type;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public double getCompleteness() {
    return completeness;
  }

  public void setCompleteness(double completeness) {
    this.completeness = completeness;
  }

  public long getPopulated() {
    return populated;
  }

  public void setPopulated(long populated) {
    this.populated = populated;
  }

  public long getNulls() {
    return nulls;
  }

  public void setNulls(long nulls) {
    this.nulls = nulls;
  }

  public long getMissing() {
    return missing;
  }

  public void setMissing(long missing) {
    this.missing = missing;
  }

  public DBObject getSummary() {
    return summary;
  }

  public void setSummary(BasicDBObject summary) {
    this.summary = summary;
  }

  public static FieldReport convert(FieldSummary fieldSummary) {
    FieldReport fieldReport = new FieldReport();
    fieldReport.setName(fieldSummary.field);
    fieldReport.setNulls(fieldSummary.nulls);

    fieldReport.setMissing(fieldSummary.missing);
    fieldReport.setPopulated(fieldSummary.populated);
    fieldReport.setCompleteness(100 * fieldSummary.populated
        / (fieldSummary.nulls + fieldSummary.missing + fieldSummary.populated));

    BasicDBObject summary = new BasicDBObject();
    for(String key : fieldSummary.summary.keySet()) {
      // Note: periods and dollar signs must be replaced for MongoDB compatibility
      summary.append(key.replace(".", "&#46;").replace("$", "&#36;"), fieldSummary.summary.get(key));
    }
    fieldReport.setSummary(summary);
    return fieldReport;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public SummaryType getType() {
    return type;
  }

  public void setType(SummaryType type) {
    this.type = type;
  }

}
