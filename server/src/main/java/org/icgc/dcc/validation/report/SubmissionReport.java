package org.icgc.dcc.validation.report;

import java.util.List;

public class SubmissionReport {
  protected List<SchemaReport> schemaReports;

  public List<SchemaReport> getSchemaReports() {
    return schemaReports;
  }

  public void setSchemaReports(List<SchemaReport> schemaReports) {
    this.schemaReports = schemaReports;
  }
}
