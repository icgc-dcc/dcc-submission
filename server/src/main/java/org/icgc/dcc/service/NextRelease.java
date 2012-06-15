package org.icgc.dcc.service;

import static com.google.common.base.Preconditions.checkArgument;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;
import org.icgc.dcc.model.dictionary.Dictionary;

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

    UpdateOperations<Release> ops =
        this.datastore.createUpdateOperations(Release.class).disableValidation()
            .set("submissions.$.state", SubmissionState.SIGNED_OFF);
    Query<Release> updateQuery =
        this.datastore.createQuery(Release.class).filter("_id", this.getRelease().getId())
            .filter("submissions.projectKey", submission.getProjectKey());

    this.datastore.update(updateQuery, ops);

    // set submission state to be signed off
    submission.setState(SubmissionState.SIGNED_OFF);
  }

  public NextRelease release(Release nextRelease) throws IllegalReleaseStateException {
    checkArgument(nextRelease != null);
    Release oldRelease = this.getRelease();
    Dictionary oldDictionary = oldRelease.getDictionary();
    if(oldDictionary == null) {
      throw new ReleaseException("Release must have associated dictionary before being completed");
    }
    if(oldRelease.equals(nextRelease)) {
      throw new ReleaseException("New release can not be the same as completed release");
    }

    nextRelease.setState(ReleaseState.OPENED);

    oldDictionary.close();
    if(nextRelease.getDictionary() == null) {
      nextRelease.setDictionary(oldRelease.getDictionary());
    }

    // save the newly created release to mongoDB
    this.datastore.save(nextRelease);

    // update the newly changed status to mongoDB
    UpdateOperations<Release> ops =
        this.datastore.createUpdateOperations(Release.class).set("state", ReleaseState.COMPLETED);

    this.datastore.update(oldRelease, ops);

    // set old release to be completed
    oldRelease.setState(ReleaseState.COMPLETED);

    return new NextRelease(nextRelease, this.datastore);
  }
}
