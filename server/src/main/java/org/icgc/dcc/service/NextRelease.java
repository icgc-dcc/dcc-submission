package org.icgc.dcc.service;

import static com.google.common.base.Preconditions.checkArgument;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;

import com.google.code.morphia.Datastore;

public class NextRelease extends HasRelease {

  private final Datastore datastore;

  public NextRelease(Release release, Datastore datastore) throws IllegalReleaseStateException {
    super(release);
    if(release.getState() != ReleaseState.OPENED) {
      throw new IllegalReleaseStateException(release, ReleaseState.OPENED);
    }
    checkArgument(datastore != null);
    this.datastore = datastore;
  }

  public void validate(Submission submission) {
    // TODO detailed implementation will be filled in after validation is done
  }

  public void signOff(Submission submission) {
    // set submission state to be signed off
    submission.setState(SubmissionState.SIGNED_OFF);

    // persist the state change with mongoDB
    this.datastore.save(submission);
  }

  public NextRelease release(Release nextRelease) throws IllegalReleaseStateException {
    // set old release to be completed
    this.getRelease().setState(ReleaseState.COMPLETED);
    nextRelease.setState(ReleaseState.OPENED);

    // persist the new release with mongoDB
    this.datastore.save(this.getRelease());
    this.datastore.save(nextRelease);

    return new NextRelease(nextRelease, this.datastore);
  }
}
