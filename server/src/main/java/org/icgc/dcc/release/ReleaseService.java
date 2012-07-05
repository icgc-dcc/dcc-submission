package org.icgc.dcc.release;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
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
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.Predicate;

public class ReleaseService extends BaseMorphiaService {

  @Inject
  public ReleaseService(Morphia morphia, Datastore datastore) {
    super(morphia, datastore);
    registerModelClasses(Release.class);
  }

  public ReleaseState nextReleaseState() throws IllegalReleaseStateException {
    return getNextRelease().getRelease().getState();
  }

  public NextRelease getNextRelease() throws IllegalReleaseStateException {
    MongodbQuery<Release> query = this.where(QRelease.release.state.eq(ReleaseState.OPENED));
    // at any time there should only be one release open which is the next release
    List<Release> nextRelease = query.list();

    checkState(nextRelease != null);
    checkState(nextRelease.size() == 1);

    return new NextRelease(nextRelease.get(0), datastore());
  }

  public MongodbQuery<Release> query() {
    return new MorphiaQuery<Release>(morphia(), datastore(), QRelease.release);
  }

  public MongodbQuery<Release> where(Predicate predicate) {
    return query().where(predicate);
  }

  public List<CompletedRelease> getCompletedReleases() throws IllegalReleaseStateException {
    List<CompletedRelease> completedReleases = new ArrayList<CompletedRelease>();

    MongodbQuery<Release> query = this.where(QRelease.release.state.eq(ReleaseState.COMPLETED));

    for(Release release : query.list()) {
      completedReleases.add(new CompletedRelease(release));
    }

    return completedReleases;
  }

  public List<HasRelease> list() {
    List<HasRelease> list = new ArrayList<HasRelease>();

    MongodbQuery<Release> query = this.query();

    for(Release release : query.list()) {
      list.add(new BaseRelease(release));
    }

    return list;
  }

  public void createInitialRelease(Release initRelease) {
    // create the first release
    datastore().save(initRelease);
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

  public List<String> getQueued() {
    return this.getSubmission(SubmissionState.QUEUED);
  }

  public boolean queue(List<String> projectKeys) {
    this.getNextRelease().release.enqueue(projectKeys);
    return this.setState(projectKeys, SubmissionState.QUEUED);
  }

  public void deleteQueuedRequest() {
    List<String> projectKeys = this.getQueued();

    this.setState(projectKeys, SubmissionState.NOT_VALIDATED);
    this.getNextRelease().release.emptyQueue();
  }

  public List<String> getSignedOff() {
    return this.getSubmission(SubmissionState.SIGNED_OFF);
  }

  public boolean signOff(List<String> projectKeys) {
    return this.setState(projectKeys, SubmissionState.SIGNED_OFF);
  }

  private List<String> getSubmission(SubmissionState state) {
    List<String> result = new ArrayList<String>();
    for(Submission submission : this.getNextRelease().getRelease().getSubmissions()) {
      if(submission.getState().equals(state)) {
        result.add(submission.getProjectKey());
      }
    }
    return result;
  }

  private boolean setState(List<String> projectKeys, SubmissionState state) {
    UpdateOperations<Release> ops;
    Query<Release> updateQuery;

    checkArgument(projectKeys != null);

    ops = datastore().createUpdateOperations(Release.class).disableValidation().set("submissions.$.state", state);
    updateQuery =
        datastore().createQuery(Release.class).filter("name =", this.getNextRelease().getRelease().getName())
            .filter("submissions.projectKey in", projectKeys);
    datastore().update(updateQuery, ops);

    return true;
  }
}
