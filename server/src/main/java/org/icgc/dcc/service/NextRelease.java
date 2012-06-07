package org.icgc.dcc.service;

import static com.google.common.base.Preconditions.checkArgument;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;

public class NextRelease extends BaseRelease {

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

    UpdateOperations<Release> ops =
        this.datastore.createUpdateOperations(Release.class).disableValidation()
            .set("submissions.$.state", SubmissionState.SIGNED_OFF);
    Query<Release> updateQuery =
        this.datastore.createQuery(Release.class).filter("name", this.getRelease().getName())
            .filter("submissions.accessionId", submission.getAccessionId());

    this.datastore.update(updateQuery, ops);

  }

  public NextRelease release(Release nextRelease) throws IllegalReleaseStateException {
    // set old release to be completed
    String curReleaseName = this.getRelease().getName();

    this.getRelease().setState(ReleaseState.COMPLETED);
    nextRelease.setState(ReleaseState.OPENED);

    // save the newly created release to mongoDB
    this.datastore.save(nextRelease);

    // update the newly changed status to mongoDB
    UpdateOperations<Release> ops =
        this.datastore.createUpdateOperations(Release.class).set("state", ReleaseState.COMPLETED);
    Query<Release> updateQuery = this.datastore.createQuery(Release.class).field("name").equal(curReleaseName);

    this.datastore.update(updateQuery, ops);

    return new NextRelease(nextRelease, this.datastore);
  }
}
