package org.icgc.dcc.release;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.DictionaryState;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.icgc.dcc.web.validator.NameValidator;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.common.base.Optional;

public class NextRelease extends BaseRelease {

  private final Datastore datastore;

  private final DccFileSystem fs;

  private final ReleaseService releaseService;

  private final DictionaryService dictionaryService;

  public NextRelease(Release release, Datastore datastore, DccFileSystem fs, ReleaseService releaseService,
      DictionaryService dictionaryService) throws IllegalReleaseStateException {
    super(release);
    if(release.getState() != ReleaseState.OPENED) {
      throw new IllegalReleaseStateException(release, ReleaseState.OPENED);
    }

    checkArgument(datastore != null);
    checkArgument(fs != null);
    checkArgument(releaseService != null);
    checkArgument(dictionaryService != null);

    this.datastore = datastore;
    this.fs = fs;
    this.releaseService = releaseService;
    this.dictionaryService = dictionaryService;
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

  public NextRelease release(String nextReleaseName) {
    checkArgument(nextReleaseName != null);

    // check for next release name
    if(!NameValidator.validate(nextReleaseName)) {
      throw new ReleaseException("Next Release name " + nextReleaseName + " is not valid");
    }

    // check for submission state to be signed off
    if(!this.canRelease()) {
      throw new ReleaseException("Release must have at least one submission that is signed off");
    }

    Release oldRelease = this.getRelease();
    Release nextRelease = new Release(nextReleaseName);
    String oldDictionaryVersion = oldRelease.getDictionaryVersion();
    String newDictionaryVersion = oldDictionaryVersion;
    if(oldDictionaryVersion == null) {
      throw new ReleaseException("Release must have associated dictionary before being completed");
    }
    if(this.datastore.createQuery(Release.class).filter("name", nextRelease.getName()).get() != null) {
      throw new ReleaseException("New release can not be the same as completed release");
    }

    nextRelease.setDictionaryVersion(newDictionaryVersion);

    nextRelease.setState(ReleaseState.OPENED);

    // dictionaries.getFromVersion(oldDictionary).close();
    UpdateOperations<Dictionary> closeDictionary =
        this.datastore.createUpdateOperations(Dictionary.class).set("state", DictionaryState.CLOSED);
    Query<Dictionary> updateDictionary =
        this.datastore.createQuery(Dictionary.class).filter("version", oldDictionaryVersion);

    this.datastore.update(updateDictionary, closeDictionary);

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

    return new NextRelease(nextRelease, this.datastore, this.fs, releaseService, dictionaryService);
  }

  public boolean canRelease() {
    Release nextRelease = this.getRelease();
    for(Submission submission : nextRelease.getSubmissions()) {
      if(submission.getState() == SubmissionState.SIGNED_OFF) {
        return true;
      }
    }

    return false;
  }

  /**
   * Does not allow to update submissions, {@code ProjectService.addProject()} must be used instead
   */
  public NextRelease update(Release updatedRelease) {
    checkArgument(updatedRelease != null);

    String updatedReleaseName = updatedRelease.getName();
    String updatedDictionaryVersion = updatedRelease.getDictionaryVersion();
    if(!NameValidator.validate(updatedReleaseName)) {
      throw new ReleaseException("Updated release name " + updatedReleaseName + " is not valid");
    }

    if(releaseService.getFromName(updatedReleaseName) != null) {
      throw new ReleaseException("New release name " + updatedReleaseName + " conflicts with an existing release");
    }

    if(updatedDictionaryVersion == null) {
      throw new ReleaseException("Release must have associated dictionary before being updated");
    }

    if(dictionaryService.getFromVersion(updatedDictionaryVersion) == null) {
      throw new ReleaseException("Release must point to an existing dictionary, no match for "
          + updatedDictionaryVersion);
    }

    Release oldRelease = this.getRelease();
    String oldReleaseName = oldRelease.getName();
    checkState(oldRelease.getState() == ReleaseState.OPENED);

    // only TWO parameters can be updated for now
    oldRelease.setName(updatedReleaseName);
    oldRelease.setDictionaryVersion(updatedDictionaryVersion);

    Query<Release> query = this.datastore.createQuery(Release.class).filter("name", oldReleaseName);
    UpdateOperations<Release> op =
        this.datastore.createUpdateOperations(Release.class).set("name", updatedReleaseName)
            .set("dictionaryVersion", updatedDictionaryVersion);
    this.datastore.update(query, op);

    return new NextRelease(oldRelease, this.datastore, this.fs, releaseService, dictionaryService);
  }
}
