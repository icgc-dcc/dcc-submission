package org.icgc.dcc.release;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.icgc.dcc.core.morphia.BaseMorphiaService;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.release.model.QRelease;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.icgc.dcc.validation.report.SubmissionReport;
import org.icgc.dcc.web.validator.NameValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;

public class ReleaseService extends BaseMorphiaService<Release> {

  private static final Logger log = LoggerFactory.getLogger(ReleaseService.class);

  private final DccFileSystem fs;

  @Inject
  public ReleaseService(Morphia morphia, Datastore datastore, DccFileSystem fs) {
    super(morphia, datastore, QRelease.release);
    this.fs = fs;
    registerModelClasses(Release.class);
  }

  public void createInitialRelease(Release initRelease) {
    // check for init release name
    if(!NameValidator.validate(initRelease.getName())) {
      throw new ReleaseException("release name " + initRelease.getName() + " is not valid");
    }
    String dictionaryVersion = initRelease.getDictionaryVersion();
    if(dictionaryVersion == null) {
      throw new ReleaseException("Dictionary version must not be null!");
    } else if(this.datastore().createQuery(Dictionary.class).filter("version", dictionaryVersion).get() == null) {
      throw new ReleaseException("Specified dictionary version not found in DB: " + dictionaryVersion);
    }
    datastore().save(initRelease);
    Set<String> projectKeys = new LinkedHashSet<String>();
    for(Submission submission : initRelease.getSubmissions()) {
      projectKeys.add(submission.getProjectKey());
    }

    this.fs.createReleaseFilesystem(initRelease, projectKeys);
  }

  public boolean hasNextRelease() {
    return this.query().where(QRelease.release.state.eq(ReleaseState.OPENED)).singleResult() != null;
  }

  public NextRelease getNextRelease() throws IllegalReleaseStateException {
    Release nextRelease = this.query().where(QRelease.release.state.eq(ReleaseState.OPENED)).singleResult();
    if(nextRelease == null) throw new IllegalStateException("no next release");
    return new NextRelease(nextRelease, datastore(), this.fs);
  }

  public List<HasRelease> list() {
    List<HasRelease> list = new ArrayList<HasRelease>();

    MongodbQuery<Release> query = this.query();

    for(Release release : query.list()) {
      list.add(new BaseRelease(release));
    }

    return list;
  }

  public List<CompletedRelease> getCompletedReleases() throws IllegalReleaseStateException {
    List<CompletedRelease> completedReleases = new ArrayList<CompletedRelease>();

    MongodbQuery<Release> query = this.where(QRelease.release.state.eq(ReleaseState.COMPLETED));

    for(Release release : query.list()) {
      completedReleases.add(new CompletedRelease(release));
    }

    return completedReleases;
  }

  public Submission getSubmission(String releaseName, String projectKey) {
    Release release = this.where(QRelease.release.name.eq(releaseName)).uniqueResult();
    checkArgument(release != null);

    Submission result = null;
    for(Submission submission : release.getSubmissions()) {
      if(submission.getProjectKey().equals(projectKey)) {
        result = submission;
        break;
      }
    }

    if(result == null) {
      throw new ReleaseException(String.format("there is no project \"%s\" associated with release \"%s\"", projectKey,
          releaseName));
    }

    return result;
  }

  public List<String> getSignedOff() {
    return this.getSubmission(SubmissionState.SIGNED_OFF);
  }

  public void signOff(List<String> projectKeys) {
    log.info("signinng off: {}", projectKeys);

    SubmissionState newState = SubmissionState.SIGNED_OFF;
    Release release = getNextRelease().getRelease();

    updateSubmisions(projectKeys, newState);
    release.removeFromQueue(projectKeys);

    this.dbUpdateSubmissions(release.getName(), release.getQueue(), projectKeys, newState);
  }

  public void deleteQueuedRequest() {
    log.info("emptying queue");

    SubmissionState newState = SubmissionState.NOT_VALIDATED;
    Release release = getNextRelease().getRelease();
    List<String> projectKeys = release.getQueue(); // TODO: what if nextrelease changes in the meantime?

    updateSubmisions(projectKeys, newState);
    release.emptyQueue();

    this.dbUpdateSubmissions(release.getName(), release.getQueue(), projectKeys, newState);
  }

  public void queue(List<String> projectKeys) {
    log.info("enqueuing: {}", projectKeys);

    SubmissionState newState = SubmissionState.QUEUED;
    Release release = this.getNextRelease().getRelease();

    updateSubmisions(projectKeys, newState);
    release.enqueue(projectKeys);

    this.dbUpdateSubmissions(release.getName(), release.getQueue(), projectKeys, newState);
  }

  public boolean hasProjectKey(List<String> projectKeys) {
    for(String projectKey : projectKeys) {
      if(!this.hasProjectKey(projectKey)) {
        return false;
      }
    }
    return true;
  }

  public boolean hasProjectKey(String projectKey) {
    Release nextRelease = this.getNextRelease().getRelease();
    for(Submission submission : nextRelease.getSubmissions()) {
      if(submission.getProjectKey().equals(projectKey)) {
        return true;
      }
    }
    return false;
  }

  public Optional<String> dequeue(String projectKey, boolean valid) {
    log.info("dequeuing: {}", projectKey);

    SubmissionState newState = valid ? SubmissionState.VALID : SubmissionState.INVALID;
    Release release = this.getNextRelease().getRelease();

    Optional<String> dequeued = release.nextInQueue();
    if(dequeued.isPresent() && dequeued.get().equals(projectKey)) {
      List<String> projectKeys = Arrays.asList(projectKey);
      dequeued = release.dequeue();
      if(dequeued.isPresent() && dequeued.get().equals(projectKey)) { // could still have changed
        updateSubmisions(projectKeys, newState);
        this.dbUpdateSubmissions(release.getName(), release.getQueue(), projectKeys, newState);
      }
    }

    return dequeued;
  }

  private void updateSubmisions(List<String> projectKeys, final SubmissionState state) {
    final String releaseName = getNextRelease().getRelease().getName();
    for(String projectKey : projectKeys) {
      getSubmission(releaseName, projectKey).setState(state);
    }
  }

  private List<String> getSubmission(final SubmissionState state) {
    List<String> projectKeys = new ArrayList<String>();
    List<Submission> submissions = this.getNextRelease().getRelease().getSubmissions();
    for(Submission submission : submissions) {
      if(state.equals(submission.getState())) {
        submission.getProjectKey();
      }
    }
    return projectKeys;
  }

  /**
   * Updates the queue then the submissions states accordingly
   */
  private void dbUpdateSubmissions(String releaseName, List<String> queue, List<String> projectKeys,
      SubmissionState newState) {
    checkArgument(releaseName != null);
    checkArgument(queue != null);

    Query<Release> updateQuery = datastore().createQuery(Release.class)//
        .filter("name = ", releaseName);
    UpdateOperations<Release> ops = datastore().createUpdateOperations(Release.class).disableValidation()//
        .set("queue", queue);
    datastore().update(updateQuery, ops);

    for(String queued : projectKeys) {
      updateQuery = datastore().createQuery(Release.class)//
          .filter("name = ", releaseName)//
          .filter("submissions.projectKey = ", queued);
      ops = datastore().createUpdateOperations(Release.class).disableValidation()//
          .set("submissions.$.state", newState);
      datastore().update(updateQuery, ops);
    }
  }

  public void UpdateSubmissionReport(String releaseName, String projectKey, SubmissionReport report) {
    Query<Release> updateQuery = datastore().createQuery(Release.class)//
        .filter("name = ", releaseName)//
        .filter("submissions.projectKey = ", projectKey);

    UpdateOperations<Release> ops = datastore().createUpdateOperations(Release.class).disableValidation()//
        .set("submissions.$.report", report);

    datastore().update(updateQuery, ops);
  }

  public Release getRelease(String releaseName) {
    Release release = where(QRelease.release.name.eq(releaseName)).singleResult();
    return release;
  }
}
