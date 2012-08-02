package org.icgc.dcc.release.model;

import org.icgc.dcc.validation.report.SubmissionReport;

import com.google.code.morphia.annotations.Embedded;

@Embedded
public class Submission {

  protected String projectKey;

  protected String projectName;

  protected SubmissionState state;

  protected SubmissionReport report;

  public SubmissionReport getReport() {
    return report;
  }

  public void setReport(SubmissionReport report) {
    this.report = report;
  }

  public SubmissionState getState() {
    return state;
  }

  public void setState(SubmissionState state) {
    this.state = state;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public void setProjectKey(String projectKey) {
    this.projectKey = projectKey;
  }
}
