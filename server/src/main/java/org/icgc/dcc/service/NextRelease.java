package org.icgc.dcc.service;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;

import com.google.inject.Inject;

public class NextRelease extends HasRelease {

  @Inject
  private ReleaseService releaseService;

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
    // set submission state to be signed off
    submission.setState(SubmissionState.SIGNED_OFF);

    // persist the state change with mongoDB
    this.releaseService.getDatastore().save(submission);
  }

  public NextRelease release(Release nextRelease) throws IllegalReleaseStateException {
    // set old release to be completed
    this.getRelease().setState(ReleaseState.COMPLETED);
    nextRelease.setState(ReleaseState.OPENED);

    // persist the new release with mongoDB
    this.releaseService.getDatastore().save(this.getRelease());
    this.releaseService.getDatastore().save(nextRelease);

    return new NextRelease(nextRelease);
  }
}
