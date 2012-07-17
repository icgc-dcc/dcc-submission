package org.icgc.dcc.validation.report;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class FieldReport {

  protected String name;

  protected double completeness;

  protected long populated;

  protected long nulls;

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

  public DBObject getSummary() {
    return summary;
  }

  public void setSummary(BasicDBObject summary) {
    this.summary = summary;
  }

}
