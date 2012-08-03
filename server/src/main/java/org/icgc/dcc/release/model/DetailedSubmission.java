package org.icgc.dcc.release.model;

public class DetailedSubmission extends Submission {
  private String projectName;

  public DetailedSubmission() {
    super();
  }

  public DetailedSubmission(Submission submission) {
    super();
    this.projectKey = submission.projectKey;
    this.state = submission.state;
    this.report = submission.report;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
}
