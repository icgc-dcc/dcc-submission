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

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((projectKey == null) ? 0 : projectKey.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if(this == obj) return true;
    if(obj == null) return false;
    if(getClass() != obj.getClass()) return false;
    Submission other = (Submission) obj;
    if(projectKey == null) {
      if(other.projectKey != null) return false;
    } else if(!projectKey.equals(other.projectKey)) return false;
    return true;
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
