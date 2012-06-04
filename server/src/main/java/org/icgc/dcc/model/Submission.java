package org.icgc.dcc.model;

import com.google.code.morphia.annotations.Embedded;

@Embedded
public class Submission {

  protected Project project;

  protected SubmissionState state;

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public SubmissionState getState() {
    return state;
  }

  public void setState(SubmissionState state) {
    this.state = state;
  }
}
