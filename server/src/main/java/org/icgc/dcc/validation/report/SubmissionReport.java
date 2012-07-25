package org.icgc.dcc.validation.report;

import java.util.List;

import com.google.code.morphia.annotations.Embedded;

@Embedded
public class SubmissionReport {
  protected List<SchemaReport> schemaReports;

  public List<SchemaReport> getSchemaReports() {
    return schemaReports;
  }

  public void setSchemaReports(List<SchemaReport> schemaReports) {
    this.schemaReports = schemaReports;
  }

  public SchemaReport getSchemaReport(String schema) {
    for(SchemaReport report : this.schemaReports) {
      if(report.getName().equals(schema)) return report;
    }
    return null;
  }
}
