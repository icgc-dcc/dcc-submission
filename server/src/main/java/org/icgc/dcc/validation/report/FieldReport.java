package org.icgc.dcc.validation.report;

import org.icgc.dcc.validation.report.BaseReportingPlanElement.FieldSummary;

import com.google.code.morphia.annotations.Embedded;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

@Embedded
public class FieldReport {

  protected String name;

  protected double completeness;

  protected long nulls;

  protected long missing;

  protected long populated;

  protected BasicDBObject summary;

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
      summary.append(key, fieldSummary.summary.get(key));
    }
    fieldReport.setSummary(summary);
    return fieldReport;
  }

}
