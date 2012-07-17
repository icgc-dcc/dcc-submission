package org.icgc.dcc.validation.report;

import java.util.List;

public class SchemaReport {

  protected String name;

  protected List<FieldReport> fieldReports;

  protected List<String> errors;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<FieldReport> getFieldReports() {
    return fieldReports;
  }

  public void setFieldReports(List<FieldReport> fieldReports) {
    this.fieldReports = fieldReports;
  }

  public List<String> getErrors() {
    return errors;
  }

  public void setErrors(List<String> errors) {
    this.errors = errors;
  }
}
