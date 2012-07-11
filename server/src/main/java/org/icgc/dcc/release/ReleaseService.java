package org.icgc.dcc.release;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.icgc.dcc.core.morphia.BaseMorphiaService;
import org.icgc.dcc.release.model.QRelease;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

public class ReleaseService extends BaseMorphiaService<Release> {

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
    return new NextRelease(nextRelease, datastore());
  }

  public ReleaseState nextReleaseState() throws IllegalReleaseStateException {
    return getNextReleaseRelease().getState();
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

    checkState(result != null);

    return result;
  }

  public List<String> getSignedOff() {
    return this.getSubmission(SubmissionState.SIGNED_OFF);
  }

  public void signOff(List<String> projectKeys) {
    SubmissionState newState = SubmissionState.SIGNED_OFF;
    Release release = getNextReleaseRelease();

    updateSubmisions(projectKeys, newState);
    release.removeFromQueue(projectKeys);

    this.dbUpdateSubmissions(release, newState);
  }

  public void deleteQueuedRequest() {
    SubmissionState newState = SubmissionState.NOT_VALIDATED;
    Release release = getNextReleaseRelease(); // TODO: what if nextrelease changes in the meantime?
    List<String> projectKeys = release.getQueue();

    updateSubmisions(projectKeys, newState);
    release.emptyQueue();

    this.dbUpdateSubmissions(release, newState);
  }

  public void queue(List<String> projectKeys) {
    SubmissionState newState = SubmissionState.QUEUED;
    Release release = this.getNextRelease().getRelease();

    updateSubmisions(projectKeys, newState);
    release.enqueue(projectKeys);

    this.dbUpdateSubmissions(release, newState);
  }

  public Optional<String> dequeue(String projectKey, boolean valid) {
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
    return getNextReleaseRelease().getQueue();
  }

  public Optional<String> getNextInQueue() {
    return getNextReleaseRelease().nextInQueue();
  }

  private void updateSubmisions(List<String> projectKeys, final SubmissionState state) {
    final String releaseName = getNextReleaseRelease().getName();
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

  private Release getNextReleaseRelease() {
    return getNextRelease().getRelease();
  }
}
