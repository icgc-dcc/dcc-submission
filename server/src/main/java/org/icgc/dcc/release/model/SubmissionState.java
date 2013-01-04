package org.icgc.dcc.release.model;

public enum SubmissionState {
  NOT_VALIDATED, QUEUED, INVALID, VALID, SIGNED_OFF, ERROR, VALIDATING;

  public boolean isReadOnly() {
    return (this == SIGNED_OFF || this == QUEUED || this == VALIDATING);
  }
}
