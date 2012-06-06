package org.icgc.dcc.model;

import com.google.code.morphia.annotations.Embedded;

@Embedded
public class Submission {

  protected String accessionId;

  protected SubmissionState state;

  public SubmissionState getState() {
    return state;
  }

  public void setState(SubmissionState state) {
    this.state = state;
  }

  public String getAccessionId() {
    return accessionId;
  }

  public void setAccessionId(String accessionId) {
    this.accessionId = accessionId;
  }
}
