package org.icgc.dcc.service;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;

public class IllegalReleaseStateException extends Exception {

  private final Release release;

  private final ReleaseState expectedState;

  public IllegalReleaseStateException(Release release, ReleaseState expectedState) {
    this.release = release;
    this.expectedState = expectedState;
  }

  @Override
  public String getMessage() {
    return "Illegal Release State:" + this.release.getState() + ", Expected State:" + this.expectedState;
  }
}
