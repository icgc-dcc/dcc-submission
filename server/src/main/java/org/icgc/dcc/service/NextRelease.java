package org.icgc.dcc.service;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.Submission;

public class NextRelease extends HasRelease {

  public NextRelease(Release release) throws IllegalReleaseStateException {
    super(release);
    if(release.getState() == ReleaseState.COMPLETED) {
      throw new IllegalReleaseStateException(release, ReleaseState.COMPLETED);
    }
  }

  public boolean validate(Submission submission) {
    // TODO detailed implementation will be filled in after validation is done
    return false;
  }
}
