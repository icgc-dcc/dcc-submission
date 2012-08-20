package org.icgc.dcc.release;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.QProject;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.DictionaryState;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.icgc.dcc.web.validator.InvalidNameException;
import org.icgc.dcc.web.validator.NameValidator;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

public class NextRelease extends BaseRelease {

  private final Datastore datastore;

  private final Morphia morphia;

  private final DccFileSystem fs;

  public NextRelease(Release release, Morphia morphia, Datastore datastore, DccFileSystem fs)
      throws IllegalReleaseStateException {
    super(release);
    if(release.getState() != ReleaseState.OPENED) {
      throw new IllegalReleaseStateException(release, ReleaseState.OPENED);
    }

    checkArgument(datastore != null);
    checkArgument(morphia != null);
    checkArgument(fs != null);

    this.datastore = datastore;
    this.morphia = morphia;
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

  public NextRelease release(String nextReleaseName) {
    checkArgument(nextReleaseName != null);

    // check for next release name
    if(NameValidator.validate(nextReleaseName) == false) {
      throw new InvalidNameException(nextReleaseName);
    }

    // check for submission state to be signed off
    if(this.canRelease() == false) {
      throw new ReleaseException("NoneSignedOff");
    }

    Release oldRelease = this.getRelease();

    if(oldRelease.getQueue().isEmpty() == false) {
      throw new ReleaseException("QueueNotEmpty");
    }

    Release nextRelease = new Release(nextReleaseName);
    String oldDictionaryVersion = oldRelease.getDictionaryVersion();
    String newDictionaryVersion = oldDictionaryVersion;
    if(oldDictionaryVersion == null) {
      throw new ReleaseException("ReleaseMissingDictionary");
    }
    if(this.datastore.createQuery(Release.class).filter("name", nextRelease.getName()).get() != null) {
      throw new ReleaseException("InvalidReleaseName");
    }
    Iterable<String> oldProjectKeys = oldRelease.getProjectKeys();
    if(oldProjectKeys == null) {
      throw new ReleaseException("InvalidReleaseState");
    }

    nextRelease.setDictionaryVersion(newDictionaryVersion);
    nextRelease.setState(ReleaseState.OPENED);
    for(Submission submission : oldRelease.getSubmissions()) {
      Submission newSubmission = new Submission(submission.getProjectKey());
      if(submission.getState() == SubmissionState.SIGNED_OFF) {
        newSubmission.setState(SubmissionState.NOT_VALIDATED);
      } else {
        newSubmission.setState(submission.getState());
        newSubmission.setReport(submission.getReport());
      }
      nextRelease.addSubmission(newSubmission);
    }

    // dictionaries.getFromVersion(oldDictionary).close();
    UpdateOperations<Dictionary> closeDictionary =
        this.datastore.createUpdateOperations(Dictionary.class).set("state", DictionaryState.CLOSED);
    Query<Dictionary> updateDictionary =
        this.datastore.createQuery(Dictionary.class).filter("version", oldDictionaryVersion);

    this.datastore.update(updateDictionary, closeDictionary);

    // save the newly created release to mongoDB
    this.datastore.save(nextRelease);

    setupFileSystem(oldRelease, nextRelease, oldProjectKeys);

    oldRelease.setState(ReleaseState.COMPLETED);
    oldRelease.setReleaseDate();
    // Non-SignedOff are removed from the old Release Object
    for(int i = oldRelease.getSubmissions().size() - 1; i >= 0; i--) {
      if(oldRelease.getSubmissions().get(i).getState() != SubmissionState.SIGNED_OFF) {
        oldRelease.getSubmissions().remove(i);
      }
    }
    // update the newly changed status to mongoDB
    UpdateOperations<Release> ops =
        this.datastore.createUpdateOperations(Release.class).set("state", ReleaseState.COMPLETED)
            .set("releaseDate", oldRelease.getReleaseDate()).set("submissions", oldRelease.getSubmissions());

    this.datastore.update(oldRelease, ops);

    return new NextRelease(nextRelease, morphia, datastore, fs);
  }

  private void setupFileSystem(Release oldRelease, Release nextRelease, Iterable<String> oldProjectKeys) {
    fs.createReleaseFilesystem(nextRelease, Sets.newLinkedHashSet(oldProjectKeys));
    ReleaseFileSystem newReleaseFilesystem = fs.getReleaseFilesystem(nextRelease);
    ReleaseFileSystem oldReleaseFilesystem = fs.getReleaseFilesystem(oldRelease);

    List<Project> projectsToMove = new ArrayList<Project>();
    for(Submission submission : oldRelease.getSubmissions()) {
      if(submission.getState() != SubmissionState.SIGNED_OFF) {
        Project project = getProjectFromKey(submission.getProjectKey());
        projectsToMove.add(project);
      }
    }
    if(projectsToMove.isEmpty() == false) {
      newReleaseFilesystem.moveFrom(oldReleaseFilesystem, ImmutableList.<Project> copyOf(projectsToMove));
    }
  }

  public Project getProjectFromKey(final String projectKey) {
    return new MorphiaQuery<Project>(morphia, datastore, QProject.project).where(QProject.project.key.eq(projectKey))
        .singleResult();
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
}
