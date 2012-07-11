package org.icgc.dcc.release;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.icgc.dcc.config.ConfigModule;
import org.icgc.dcc.core.CoreModule;
import org.icgc.dcc.core.morphia.BaseMorphiaService;
import org.icgc.dcc.core.morphia.MorphiaModule;
import org.icgc.dcc.dictionary.DictionaryModule;
import org.icgc.dcc.filesystem.FileSystemModule;
import org.icgc.dcc.http.HttpModule;
import org.icgc.dcc.http.jersey.JerseyModule;
import org.icgc.dcc.release.model.QRelease;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.icgc.dcc.sftp.SftpModule;
import org.icgc.dcc.shiro.ShiroModule;
import org.icgc.dcc.validation.ValidationModule;
import org.icgc.dcc.web.WebModule;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.typesafe.config.ConfigFactory;

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
    return getNextRelease().getRelease().getState();
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
    return (List<String>) this.getSubmission(SubmissionState.SIGNED_OFF);
  }

  public void signOff(List<String> projectKeys) {
    SubmissionState newState = SubmissionState.SIGNED_OFF;

    Release release = getNextRelease().getRelease();
    release.removeFromQueue(projectKeys);
    updateSubmisions(projectKeys, newState);

    this.dbUpdateQueueAndSubmissions(projectKeys, newState);
  }

  public List<String> getQueued() {
    return getNextRelease().getRelease().getQueue(); // TODO: safe to consider up-to-date at all time?
  }

  public void deleteQueuedRequest() {
    SubmissionState newState = SubmissionState.NOT_VALIDATED;

    Release release = getNextRelease().getRelease();// TODO: what if nextrelease changes in the meantime?
    List<String> projectKeys = release.getQueue();
    release.emptyQueue();
    updateSubmisions(projectKeys, newState);

    this.dbUpdateQueueAndSubmissions(projectKeys, newState);
  }

  public void queue(List<String> projectKeys) {
    SubmissionState newState = SubmissionState.QUEUED;

    Release release = this.getNextRelease().getRelease();
    release.enqueue(projectKeys);
    updateSubmisions(projectKeys, newState);

    this.dbUpdateQueueAndSubmissions(projectKeys, newState);
  }

  public Optional<String> dequeue(String projectKey, boolean valid) {
    SubmissionState newState = valid ? SubmissionState.VALID : SubmissionState.INVALID;

    Release release = this.getNextRelease().getRelease();
    release.removeFromQueue(Arrays.asList(projectKey));
    updateSubmisions(Arrays.asList(projectKey), newState);

    Optional<String> dequeued = Optional.<String> absent();// TODO
    return dequeued;
  }

  private void updateSubmisions(List<String> projectKeys, final SubmissionState state) {
    final String releaseName = getNextRelease().getRelease().getName();
    Iterables.transform(projectKeys,//
        new Function<String, String>() { // TODO: is there a guava procedure equivalent?
          @Override
          public String apply(String input) {
            Submission submission = getSubmission(releaseName, input);
            submission.setState(state);// TODO: check if has indeed changed?
            return input;
          }
        });
  }

  private Iterable<String> getSubmission(final SubmissionState state) {
    List<Submission> submissions = this.getNextRelease().getRelease().getSubmissions();
    Iterable<Submission> matchingSubmissions = Iterables.filter(submissions, new Predicate<Submission>() {
      @Override
      public boolean apply(Submission input) {
        return input.getState().equals(state);
      }
    });
    Iterable<String> projectKeys = Iterables.transform(matchingSubmissions, new Function<Submission, String>() {
      @Override
      public String apply(Submission input) {
        return input.getProjectKey();
      }
    });
    return projectKeys;
  }

  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new ConfigModule(ConfigFactory.load())//
        , new CoreModule()//
        , new HttpModule()//
        , new JerseyModule()//
        , new WebModule()//
        , new MorphiaModule()//
        , new ShiroModule()//
        , new FileSystemModule()//
        , new SftpModule()//
        , new DictionaryModule()//
        , new ReleaseModule()//
        , new ValidationModule()//
        );

    ReleaseService releaseService = injector.getInstance(ReleaseService.class);
    NextRelease nextRelease = releaseService.getNextRelease();
    Release release = nextRelease.getRelease();
    List<Submission> submissions = release.getSubmissions();

    System.out.println(release.getName());
    System.out.println(release.getQueue());
    System.out.println(submissions.size());
    for(Submission s : submissions) {
      System.out.println("\t" + s.getProjectKey() + "-" + s.getState());
    }

    releaseService.dbUpdateQueueAndSubmissions(Arrays.asList("project1", "project2"), SubmissionState.VALID);
    release = releaseService.getNextRelease().getRelease();
    submissions = release.getSubmissions();

    System.out.println(release.getName());
    System.out.println(release.getQueue());
    System.out.println(submissions.size());
    for(Submission s : submissions) {
      System.out.println("\t" + s.getProjectKey() + "-" + s.getState());
    }
  }

  private void dbUpdateQueueAndSubmissions(List<String> projectKeys, SubmissionState state) {
    checkArgument(projectKeys != null);

    Query<Release> updateQuery = datastore().createQuery(Release.class)//
        .filter("name =", this.getNextRelease().getRelease().getName())//
        .filter("submissions.projectKey in", projectKeys);

    UpdateOperations<Release> ops = datastore().createUpdateOperations(Release.class).disableValidation()//
        .set("queue", projectKeys)//
        .set("submissions.$.state", state);

    datastore().update(updateQuery, ops);
  }
}
