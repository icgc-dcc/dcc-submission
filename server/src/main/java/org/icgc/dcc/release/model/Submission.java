package org.icgc.dcc.release.model;

import java.util.Date;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.icgc.dcc.validation.report.SubmissionReport;

import com.google.code.morphia.annotations.Embedded;

@Embedded
public class Submission {

  @NotNull
  protected String projectKey;

  @NotNull
  protected String projectName;

  protected Date lastUpdated;

  protected SubmissionState state;

  @Valid
  protected SubmissionReport report;

  public Submission() {
    super();
  }

  public Submission(String projectKey) {
    super();
    this.projectKey = projectKey;
    this.state = SubmissionState.NOT_VALIDATED;
    this.lastUpdated = new Date();
  }

  /**
   * @return the lastUpdated
   */
  public Date getLastUpdated() {
    return lastUpdated;
  }

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
    this.lastUpdated = new Date();
    this.state = state;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public void setProjectKey(String projectKey) {
    this.projectKey = projectKey;
  }
}
