package org.icgc.dcc.release;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import org.icgc.dcc.core.model.InvalidStateException;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.QProject;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.DictionaryState;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.release.model.QueuedProject;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.icgc.dcc.web.validator.InvalidNameException;
import org.icgc.dcc.web.validator.NameValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

public class NextRelease extends BaseRelease {

  private static final Logger log = LoggerFactory.getLogger(NextRelease.class);

  private final DccLocking dccLocking;

  public NextRelease(final DccLocking dccLocking, final Release release, final Morphia morphia,
      final Datastore datastore, final DccFileSystem fs) throws IllegalReleaseStateException {
    super(release, morphia, datastore, fs);
    checkArgument(dccLocking != null);

    this.dccLocking = dccLocking; // TODO: moveup (DCC-685)?
    dccLocking.setDatastore(datastore);

    if(release.getState() != ReleaseState.OPENED) {
      throw new IllegalReleaseStateException(release, ReleaseState.OPENED);
    }
  }

  public List<String> getQueued() {
    return getRelease().getQueuedProjectKeys();
  }

  public Optional<QueuedProject> getNextInQueue() {
    return getRelease().nextInQueue();
  }

  public NextRelease release(final String nextReleaseName) throws InvalidStateException {
    checkArgument(nextReleaseName != null);

    // check for next release name
    if(NameValidator.validate(nextReleaseName) == false) {
      throw new InvalidNameException(nextReleaseName);
    }

    Release nextRelease = null;
    Release oldRelease = dccLocking.acquireReleasingLock(); // TODO: for now nothing checks for it (DCC-685); also
                                                            // consider reentrance out of safety
    try {
      if(oldRelease == null) { // just in case (can't really happen)
        log.error("could not acquire lock on release");
        throw new ReleaseException("ReleaseException");
      }
      if(oldRelease.equals(this.getRelease()) == false) { // just in case (can't really happen)
        log.error(oldRelease + " != " + this.getRelease());
        throw new ReleaseException("ReleaseException");
      }
      if(atLeastOneSignedOff(oldRelease) == false) { // check for signed-off submission states (must have at least one)
        log.error("no signed off project in " + oldRelease);
        throw new InvalidStateException("SignedOffSubmissionRequired");
      }
      if(oldRelease.getQueue().isEmpty() == false) {
        log.error("some projects are still enqueue in " + oldRelease);
        throw new InvalidStateException("QueueNotEmpty");
      }

      String dictionaryVersion = oldRelease.getDictionaryVersion();
      if(dictionaryVersion == null) {
        log.error("could not find a dictionary matching " + dictionaryVersion);
        throw new InvalidStateException("ReleaseMissingDictionary"); // TODO: new kind of exception rather?
      }
      if(forName(nextReleaseName) != null) {
        log.error("found a conflicting release for name " + nextReleaseName);
        throw new InvalidStateException("DuplicateReleaseName");
      }

      // critical operations
      nextRelease = createNextRelease(nextReleaseName, oldRelease, dictionaryVersion);
      setupNextReleaseFileSystem(oldRelease, nextRelease, oldRelease.getProjectKeys()); // TODO: fix situation regarding
                                                                                        // aborting fs operations?
      closeDictionary(dictionaryVersion);
      completeOldRelease(oldRelease);
    } finally {
      Release relinquishedRelease = dccLocking.relinquishReleasingLock();
      if(relinquishedRelease == null || //
          relinquishedRelease.equals(oldRelease) == false) { // just in case
        log.error("could not relinquish lock on release {}, obtaining {}",
            new Object[] { oldRelease, relinquishedRelease });
        throw new ReleaseException("ReleaseException");
      }
    }

    return new NextRelease(dccLocking, nextRelease, morphia(), datastore(), fileSystem());
  }

  private Release createNextRelease(final String name, final Release oldRelease, final String dictionaryVersion) {
    Release nextRelease = new Release(name);
    nextRelease.setDictionaryVersion(dictionaryVersion);
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
    datastore().save(nextRelease); // TODO: put in ReleaseService?
    return nextRelease;
  }

  private void setupNextReleaseFileSystem(final Release oldRelease, final Release nextRelease,
      final Iterable<String> oldProjectKeys) {
    fileSystem().createReleaseFilesystem(nextRelease, Sets.newLinkedHashSet(oldProjectKeys));
    ReleaseFileSystem newReleaseFilesystem = fileSystem().getReleaseFilesystem(nextRelease);
    ReleaseFileSystem oldReleaseFilesystem = fileSystem().getReleaseFilesystem(oldRelease);
    newReleaseFilesystem.moveFrom(oldReleaseFilesystem, getProjectsToMove(oldRelease));
  }

  /**
   * Idempotent.
   */
  private void closeDictionary(final String oldDictionaryVersion) { // TODO: move to dictionary service?
    datastore().findAndModify( //
        datastore().createQuery(Dictionary.class) //
            .filter("version", oldDictionaryVersion), //
        datastore().createUpdateOperations(Dictionary.class) //
            .set("state", DictionaryState.CLOSED));
  }

  private void completeOldRelease(final Release oldRelease) {
    oldRelease.setState(ReleaseState.COMPLETED);
    oldRelease.setReleaseDate();
    List<Submission> submissions = oldRelease.getSubmissions();
    for(int i = submissions.size() - 1; i >= 0; i--) {
      if(submissions.get(i).getState() != SubmissionState.SIGNED_OFF) {
        submissions.remove(i);
      }
    }

    datastore().findAndModify( //
        query() //
            .filter("name", oldRelease.getName()), //
        update() //
            .set("state", oldRelease.getState()) //
            .set("releaseDate", oldRelease.getReleaseDate()) //
            .set("submissions", submissions));
  }

  boolean atLeastOneSignedOff(Release release) {
    for(Submission submission : release.getSubmissions()) {
      if(submission.getState() == SubmissionState.SIGNED_OFF) {
        return true;
      }
    }
    return false;
  }

  private ImmutableList<Project> getProjectsToMove(final Release oldRelease) {
    List<Project> projects = Lists.newArrayList();
    for(Submission submission : oldRelease.getSubmissions()) {
      if(submission.getState() != SubmissionState.SIGNED_OFF) {
        Project project = projectFromKey(submission.getProjectKey());
        projects.add(project);
      }
    }
    return ImmutableList.<Project> copyOf(projects);
  }

  private Release forName(final String nextReleaseName) { // TODO: put in ReleaseService?
    return query().filter("name", nextReleaseName).get();
  }

  private Query<Release> query() {
    return datastore().createQuery(Release.class);
  }

  private UpdateOperations<Release> update() {
    return datastore().createUpdateOperations(Release.class);
  }

  private Project projectFromKey(final String projectKey) { // TODO: move to project service?
    return new MorphiaQuery<Project>(morphia(), datastore(), QProject.project).where(
        QProject.project.key.eq(projectKey)).singleResult();
  }
}
