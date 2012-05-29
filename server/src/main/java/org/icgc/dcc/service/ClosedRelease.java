package org.icgc.dcc.service;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;

public class ClosedRelease extends NextRelease {

  public ClosedRelease(Release release) throws IllegalReleaseStateException {
    super(release);
    if(release.getState() != ReleaseState.CLOSED) {
      throw new IllegalReleaseStateException(release, ReleaseState.CLOSED);
    }
  }

  public OpenedRelease release(Release nextRelease) throws IllegalReleaseStateException {
    return new OpenedRelease(nextRelease);
  }

  public void signOff(Submission submission) {
    submission.setState(SubmissionState.SIGNED_OFF);
  }
}
