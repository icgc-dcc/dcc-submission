package org.icgc.dcc.model;

import com.google.code.morphia.annotations.Embedded;

@Embedded
public class Submission {

  protected String projectKey;

  protected SubmissionState state;

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
