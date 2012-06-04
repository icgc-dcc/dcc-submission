package org.icgc.dcc.service;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;

public class CompletedRelease extends BaseRelease {

  CompletedRelease(Release release) throws IllegalReleaseStateException {
    super(release);
    if(release.getState() != ReleaseState.COMPLETED) {
      throw new IllegalReleaseStateException(release, ReleaseState.COMPLETED);
    }
  }
}
