package org.icgc.dcc.release;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.DictionaryState;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.common.base.Optional;

public class NextRelease extends BaseRelease {

  private final Datastore datastore;

  private final DccFileSystem fs;

  public NextRelease(Release release, Datastore datastore, DccFileSystem fs) throws IllegalReleaseStateException {
    super(release);
    if(release.getState() != ReleaseState.OPENED) {
      throw new IllegalReleaseStateException(release, ReleaseState.OPENED);
    }
    checkArgument(datastore != null);
    checkArgument(fs != null);
    this.datastore = datastore;
    this.fs = fs;
  }

  public List<String> getQueued() {
    return getRelease().getQueue();
  }

  public Optional<String> getNextInQueue() {
    return getRelease().nextInQueue();
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
    String oldDictionary = oldRelease.getDictionaryVersion();
    if(oldDictionary == null) {
      throw new ReleaseException("Release must have associated dictionary before being completed");
    }
    if(this.datastore.createQuery(Release.class).filter("name", nextRelease.getName()).get() != null) {
      throw new ReleaseException("New release can not be the same as completed release");
    }

    nextRelease.setState(ReleaseState.OPENED);

    // dictionaries.getFromVersion(oldDictionary).close();
    UpdateOperations<Dictionary> closeDictionary =
        this.datastore.createUpdateOperations(Dictionary.class).set("state", DictionaryState.CLOSED);
    Query<Dictionary> updateDictionary = this.datastore.createQuery(Dictionary.class).filter("version", oldDictionary);

    this.datastore.update(updateDictionary, closeDictionary);

    if(nextRelease.getDictionaryVersion() == null) {
      nextRelease.setDictionaryVersion(oldRelease.getDictionaryVersion());
    }

    // save the newly created release to mongoDB
    this.datastore.save(nextRelease);

    Set<String> projectKeys = new HashSet<String>();
    for(Submission submission : nextRelease.getSubmissions()) {
      projectKeys.add(submission.getProjectKey());
    }
    this.fs.createReleaseFilesystem(nextRelease, projectKeys);

    oldRelease.setState(ReleaseState.COMPLETED);
    oldRelease.setReleaseDate();
    // update the newly changed status to mongoDB
    UpdateOperations<Release> ops =
        this.datastore.createUpdateOperations(Release.class).set("state", ReleaseState.COMPLETED)
            .set("releaseDate", oldRelease.getReleaseDate());

    this.datastore.update(oldRelease, ops);

    return new NextRelease(nextRelease, this.datastore, this.fs);
  }
}
