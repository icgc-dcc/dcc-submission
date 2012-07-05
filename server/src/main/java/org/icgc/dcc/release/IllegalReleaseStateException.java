package org.icgc.dcc.release;

import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;

public class IllegalReleaseStateException extends RuntimeException {
  private static final long serialVersionUID = 3199743377772054709L;

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
