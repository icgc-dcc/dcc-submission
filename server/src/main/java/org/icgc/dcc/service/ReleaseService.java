package org.icgc.dcc.service;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.model.QRelease;
import org.icgc.dcc.model.QSubmission;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Predicate;

public class ReleaseService {

  private final Morphia morphia;

  private final Datastore datastore;

  @Inject
  public ReleaseService(Morphia morphia, Datastore datastore) {
    super();
    checkArgument(morphia != null);
    checkArgument(datastore != null);
    this.morphia = morphia;
    this.datastore = datastore;
  }

  public ReleaseState nextReleaseState() throws IllegalReleaseStateException {
    return getNextRelease().getRelease().getState();
  }

  public NextRelease getNextRelease() throws IllegalReleaseStateException {
    MongodbQuery<Release> query = this.where(QRelease.release.state.eq(ReleaseState.OPENED));
    // at any time there should only be one release open which is the next release
    checkArgument(query.list().size() == 1);

    return new NextRelease(query.list().get(0), datastore);
  }

  public MongodbQuery<Release> query() {
    return new MorphiaQuery<Release>(morphia, datastore, QRelease.release);
  }

  public MongodbQuery<Release> where(Predicate predicate) {
    return query().where(predicate);
  }

  public Datastore getDatastore() {
    return this.datastore;
  }

  public Morphia getMorphia() {
    return this.morphia;
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

  public List<Submission> getQueued() {
    MorphiaQuery<Submission> query =
        new MorphiaQuery<Submission>(morphia, datastore, (EntityPath<Submission>) QSubmission.submission);

    List<Submission> submissions = query.where(QSubmission.submission.state.eq(SubmissionState.QUEUED)).list();
    return submissions;
  }

  public List<Submission> getSubmissionFromAccessionId(String accessionId) {
    MorphiaQuery<Submission> query =
        new MorphiaQuery<Submission>(morphia, datastore, (EntityPath<Submission>) QSubmission.submission);

    List<Submission> submissions = query.where(QSubmission.submission.project.accessionId.eq(accessionId)).list();
    return submissions;
  }

  public List<Submission> getSignedOff() {
    MorphiaQuery<Submission> query =
        new MorphiaQuery<Submission>(morphia, datastore, (EntityPath<Submission>) QSubmission.submission);

    List<Submission> submissions = query.where(QSubmission.submission.state.eq(SubmissionState.SIGNED_OFF)).list();
    return submissions;
  }
}
