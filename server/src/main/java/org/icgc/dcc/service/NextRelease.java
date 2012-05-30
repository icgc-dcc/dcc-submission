package org.icgc.dcc.service;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;

public class NextRelease extends HasRelease {

  public NextRelease(Release release) throws IllegalReleaseStateException {
    super(release);
    if(release.getState() != ReleaseState.OPENED) {
      throw new IllegalReleaseStateException(release, ReleaseState.OPENED);
    }
  }

  public void validate(Submission submission) {
    // TODO detailed implementation will be filled in after validation is done
  }

  public void signOff(Submission submission) {
    submission.setState(SubmissionState.SIGNED_OFF);
  }

  public NextRelease release(Release nextRelease) throws IllegalReleaseStateException {
    // set old release to be completed
    nextRelease.setState(ReleaseState.COMPLETED);
    // create new next release and return
    Release newNextRelease = new Release();
    return new NextRelease(newNextRelease);
  }
}
