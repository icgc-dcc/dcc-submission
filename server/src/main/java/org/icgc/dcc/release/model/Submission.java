package org.icgc.dcc.release.model;

import java.util.Date;

import org.icgc.dcc.validation.report.SubmissionReport;

import com.google.code.morphia.annotations.Embedded;
import com.google.common.base.Objects;

@Embedded
public class Submission {

  protected String projectKey;

  protected String projectName;

  protected Date lastUpdated;

  protected SubmissionState state;

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

  @Override
  public boolean equals(Object obj) { // TODO: hashCode (if we need hashes)
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
    }
    if(getClass() != obj.getClass()) {
      return false;
    }
    final Submission other = (Submission) obj;
    return Objects.equal(this.projectKey, other.projectKey);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(Submission.class) //
        .add("projectKey", this.projectKey) //
        .add("projectName", this.projectName) //
        .add("lastUpdated", this.lastUpdated) //
        .add("state", this.state) //
        .add("report", this.report) // TODO: toString for SubmissionReport
        .toString();
  }
}
