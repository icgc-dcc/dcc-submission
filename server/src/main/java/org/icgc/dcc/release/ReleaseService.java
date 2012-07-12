package org.icgc.dcc.release;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.icgc.dcc.core.morphia.BaseMorphiaService;
import org.icgc.dcc.release.model.QRelease;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

public class ReleaseService extends BaseMorphiaService<Release> {

  private static final Logger log = LoggerFactory.getLogger(ReleaseService.class);

  @Inject
  public ReleaseService(Morphia morphia, Datastore datastore) {
    super(morphia, datastore, QRelease.release);
    registerModelClasses(Release.class);
  }

  @Override
  public MongodbQuery<Release> query() {
    return new MorphiaQuery<Release>(morphia(), datastore(), QRelease.release);
  }

  @Override
  public MongodbQuery<Release> where(com.mysema.query.types.Predicate predicate) {
    return query().where(predicate);
  }

  public void createInitialRelease(Release initRelease) {
    datastore().save(initRelease);
  }

  public NextRelease getNextRelease() throws IllegalReleaseStateException {
    Release nextRelease = this.query().where(QRelease.release.state.eq(ReleaseState.OPENED)).singleResult();
    return nextRelease != null ? new NextRelease(nextRelease, datastore()) : null;
  }

  public ReleaseState nextReleaseState() throws IllegalReleaseStateException {
    return getNextReleaseRelease().get().getState();// TODO: handle no releases
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
    Release release = getNextReleaseRelease().get();// TODO: handle no releases

    updateSubmisions(projectKeys, newState);
    release.removeFromQueue(projectKeys);

    this.dbUpdateSubmissions(release, newState);
  }

  public void deleteQueuedRequest() {
    log.info("emptying queue");

    SubmissionState newState = SubmissionState.NOT_VALIDATED;
    Release release = getNextReleaseRelease().get();// TODO: handle no releases
    List<String> projectKeys = release.getQueue(); // TODO: what if nextrelease changes in the meantime?

    updateSubmisions(projectKeys, newState);
    release.emptyQueue();

    this.dbUpdateSubmissions(release, newState);
  }

  public void queue(List<String> projectKeys) {
    log.info("enqueuing: {}", projectKeys);

    SubmissionState newState = SubmissionState.QUEUED;
    Release release = this.getNextRelease().getRelease();

    updateSubmisions(projectKeys, newState);
    release.enqueue(projectKeys);

    this.dbUpdateSubmissions(release, newState);
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
        this.dbUpdateSubmissions(release, newState);
      }
    }

    return dequeued;
  }

  public List<String> getQueued() {
    Optional<Release> release = getNextReleaseRelease();
    return release.isPresent() ? release.get().getQueue() : null;// TODO: handle no releases
  }

  public Optional<String> getNextInQueue() {
    Optional<Release> release = getNextReleaseRelease();
    return release.isPresent() ? release.get().nextInQueue() : Optional.<String> absent();
  }

  private void updateSubmisions(List<String> projectKeys, final SubmissionState state) {
    final String releaseName = getNextReleaseRelease().get().getName();// TODO: handle no releases
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
  private void dbUpdateSubmissions(Release release, SubmissionState newState) {
    String releaseName = release.getName();
    List<String> queue = release.getQueue();
    checkArgument(queue != null);

    Query<Release> updateQuery = datastore().createQuery(Release.class)//
        .filter("name = ", releaseName);
    UpdateOperations<Release> ops = datastore().createUpdateOperations(Release.class).disableValidation()//
        .set("queue", queue);
    datastore().update(updateQuery, ops);

    for(String queued : queue) {
      updateQuery = datastore().createQuery(Release.class)//
          .filter("name = ", releaseName)//
          .filter("submissions.projectKey = ", queued);
      ops = datastore().createUpdateOperations(Release.class).disableValidation()//
          .set("submissions.$.state", newState);
      datastore().update(updateQuery, ops);
    }
  }

  private Optional<Release> getNextReleaseRelease() {
    NextRelease nextRelease = getNextRelease();
    return null != nextRelease ? Optional.<Release> of(nextRelease.getRelease()) : Optional.<Release> absent();
  }
}
