package org.icgc.dcc.service;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;

public class OpenedRelease extends NextRelease {

  public OpenedRelease(Release release) throws IllegalReleaseStateException {
    super(release);
    if(release.getState() != ReleaseState.OPENED) {
      throw new IllegalReleaseStateException(release, ReleaseState.OPENED);
    }
  }

  public ClosedRelease close() throws IllegalReleaseStateException {
    this.release.setState(ReleaseState.CLOSED);
    return new ClosedRelease(this.release);
  }
}
