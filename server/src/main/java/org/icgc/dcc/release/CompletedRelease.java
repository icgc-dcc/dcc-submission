package org.icgc.dcc.release;

import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;

public class CompletedRelease extends BaseRelease {

  CompletedRelease(Release release) throws IllegalReleaseStateException {
    super(release);
    if(release.getState() != ReleaseState.COMPLETED) {
      throw new IllegalReleaseStateException(release, ReleaseState.COMPLETED);
    }
  }
}
