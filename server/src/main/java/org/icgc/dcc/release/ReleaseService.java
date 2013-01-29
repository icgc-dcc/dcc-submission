package org.icgc.dcc.release;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.hadoop.fs.Path;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.core.MailUtils;
import org.icgc.dcc.core.model.BaseEntity;
import org.icgc.dcc.core.model.DccConcurrencyException;
import org.icgc.dcc.core.model.DccModelOptimisticLockException;
import org.icgc.dcc.core.model.InvalidStateException;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.QProject;
import org.icgc.dcc.core.morphia.BaseMorphiaService;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.QDictionary;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.filesystem.SubmissionDirectory;
import org.icgc.dcc.filesystem.SubmissionFile;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.release.model.DetailedSubmission;
import org.icgc.dcc.release.model.QRelease;
import org.icgc.dcc.release.model.QueuedProject;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.ReleaseView;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.icgc.dcc.shiro.AuthorizationPrivileges;
import org.icgc.dcc.validation.report.SubmissionReport;
import org.icgc.dcc.web.ServerErrorCode;
import org.icgc.dcc.web.validator.InvalidNameException;
import org.icgc.dcc.web.validator.NameValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.code.morphia.query.UpdateResults;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.typesafe.config.Config;

public class ReleaseService extends BaseMorphiaService<Release> {

  private static final Logger log = LoggerFactory.getLogger(ReleaseService.class);

  private final DccLocking dccLocking;

  private final DccFileSystem fs;

  private final Config config;

  @Inject
  public ReleaseService(DccLocking dccLocking, Morphia morphia, Datastore datastore, DccFileSystem fs, Config config) {
    super(morphia, datastore, QRelease.release);
    checkArgument(dccLocking != null);
    checkArgument(fs != null);
    checkArgument(config != null);
    this.dccLocking = dccLocking;
    this.fs = fs;
    this.config = config;
    registerModelClasses(Release.class);
  }

  public List<Release> getReleases(Subject subject) {
    log.debug("getting releases for {}", subject.getPrincipal());

    List<Release> releases = query().list();
    log.debug("#releases: ", releases.size());

    // filter out all the submissions that the current user can not see
    for(Release release : releases) {
      List<Submission> newSubmissions = Lists.newArrayList();
      for(Submission submission : release.getSubmissions()) {
        String projectKey = submission.getProjectKey();
        if(subject.isPermitted(AuthorizationPrivileges.projectViewPrivilege(projectKey))) {
          newSubmissions.add(submission);
        }
      }
      release.getSubmissions().clear(); // TODO: should we manipulate release this way? consider creating DTO?
      release.getSubmissions().addAll(newSubmissions);
    }
    log.debug("#releases visible: ", releases.size());

    return releases;
  }

  public void createInitialRelease(Release initRelease) {
    // check for init release name
    if(!NameValidator.validate(initRelease.getName())) {
      throw new InvalidNameException(initRelease.getName());
    }
    String dictionaryVersion = initRelease.getDictionaryVersion();
    if(dictionaryVersion == null) {
      throw new ReleaseException("Dictionary version must not be null!");
    } else if(buildDictionaryVersionQuery(dictionaryVersion).get() == null) {
      throw new ReleaseException("Specified dictionary version not found in DB: " + dictionaryVersion);
    }
    // Just use name and dictionaryVersion from incoming json
    Release nextRelease = new Release(initRelease.getName());
    nextRelease.setDictionaryVersion(dictionaryVersion);
    datastore().save(nextRelease);
    // after initial release, create initial file system
    Set<String> projects = Sets.newHashSet();
    fs.ensureReleaseFilesystem(nextRelease, projects);
  }

  public boolean hasNextRelease() {
    return this.query().where(QRelease.release.state.eq(ReleaseState.OPENED)).singleResult() != null;
  }

  public Release getFromName(String releaseName) {
    Release release = this.query().where(QRelease.release.name.eq(releaseName)).uniqueResult();

    return release;
  }

  public ReleaseView getReleaseView(String releaseName, Subject user) {
    Release release = this.query().where(QRelease.release.name.eq(releaseName)).uniqueResult();

    if(release == null) {
      return null;
    }
    // populate project name for submissions
    List<Project> projects = this.getProjects(release, user);
    List<Entry<String, String>> projectEntries = buildProjectEntries(projects);
    Map<String, List<SubmissionFile>> submissionFilesMap = buildSubmissionFilesMap(releaseName, release);
    ReleaseView releaseView = new ReleaseView(release, projectEntries, submissionFilesMap);

    return releaseView;
  }

  public NextRelease getNextRelease() throws IllegalReleaseStateException {
    Release nextRelease = this.query().where(QRelease.release.state.eq(ReleaseState.OPENED)).singleResult();
    if(nextRelease == null) {
      throw new IllegalStateException("no next release");
    }
    return new NextRelease(dccLocking, nextRelease, morphia(), datastore(), this.fs);
  }

  public List<HasRelease> list() {
    List<HasRelease> list = new ArrayList<HasRelease>();

    MongodbQuery<Release> query = this.query();

    for(Release release : query.list()) {
      if(release.getState() == ReleaseState.OPENED) {
        list.add(new NextRelease(dccLocking, release, morphia(), datastore(), fs));
      } else {
        list.add(new CompletedRelease(release, morphia(), datastore(), fs));
      }
    }

    return list;
  }

  public CompletedRelease getCompletedRelease(String releaseName) throws IllegalReleaseStateException {
    MongodbQuery<Release> query =
        this.where(QRelease.release.state.eq(ReleaseState.COMPLETED).and(QRelease.release.name.eq(releaseName)));
    Release release = query.uniqueResult();
    if(release == null) {
      throw new IllegalArgumentException("release " + releaseName + " is not complete");
    }
    return new CompletedRelease(release, morphia(), datastore(), fs);
  }

  public Submission getSubmission(String releaseName, String projectKey) {
    Release release = this.where(QRelease.release.name.eq(releaseName)).uniqueResult();
    checkArgument(release != null);

    return this.getSubmission(release, projectKey);
  }

  public DetailedSubmission getDetailedSubmission(String releaseName, String projectKey) {
    DetailedSubmission detailedSubmission = new DetailedSubmission(this.getSubmission(releaseName, projectKey));
    detailedSubmission.setProjectName(this.getProject(projectKey).getName());
    detailedSubmission.setSubmissionFiles(getSubmissionFiles(releaseName, projectKey));
    return detailedSubmission;
  }

  public List<CompletedRelease> getCompletedReleases() throws IllegalReleaseStateException {
    List<CompletedRelease> completedReleases = new ArrayList<CompletedRelease>();

    MongodbQuery<Release> query = this.where(QRelease.release.state.eq(ReleaseState.COMPLETED));

    for(Release release : query.list()) {
      completedReleases.add(new CompletedRelease(release, morphia(), datastore(), fs));
    }

    return completedReleases;
  }

  private Submission getSubmission(Release release, String projectKey) {
    for(Submission submission : release.getSubmissions()) {
      if(submission.getProjectKey().equals(projectKey)) {
        return submission;
      }
    }

    throw new ReleaseException(String.format("there is no project \"%s\" associated with release \"%s\"", projectKey,
        release.getName()));
  }

  public Release getRelease(String releaseName) {
    return this.where(QRelease.release.name.eq(releaseName)).uniqueResult();
  }

  public List<String> getSignedOff() {
    return this.getSubmission(SubmissionState.SIGNED_OFF);
  }

  public void signOff(Release nextRelease, List<String> projectKeys, String user) //
      throws InvalidStateException, DccModelOptimisticLockException {

    String nextReleaseName = nextRelease.getName();
    log.info("signing off {} for {}", projectKeys, nextReleaseName);

    // update release object
    SubmissionState expectedState = SubmissionState.VALID;
    nextRelease.removeFromQueue(projectKeys);
    for(String projectKey : projectKeys) {
      Submission submission = fetchAndCheckSubmission(nextRelease, projectKey, expectedState);
      submission.setState(SubmissionState.SIGNED_OFF);
    }

    updateRelease(nextReleaseName, nextRelease);

    // TODO: synchronization (DCC-685), may require cleaning up the FS abstraction (do we really need the project object
    // or is the projectKey sufficient?)
    // remove .validation folder from the Submission folder
    ReleaseFileSystem releaseFS = this.fs.getReleaseFilesystem(nextRelease);
    List<Project> projects = this.getProjects(projectKeys);
    for(Project project : projects) {
      SubmissionDirectory submissionDirectory = releaseFS.getSubmissionDirectory(project);
      submissionDirectory.removeSubmissionDir();
    }

    // after sign off, send a email to DCC support
    MailUtils.sendEmail(this.config, //
        String.format("Signed off Projects: %s", projectKeys), //
        String.format(config.getString("mail.signoff_body"), user, projectKeys, nextReleaseName));

    log.info("signed off {} for {}", projectKeys, nextReleaseName);
  }

  public void deleteQueuedRequest() {
    log.info("emptying queue");

    SubmissionState newState = SubmissionState.NOT_VALIDATED;
    Release release = getNextRelease().getRelease();
    List<String> projectKeys = release.getQueuedProjectKeys(); // TODO: what if nextrelease changes in the meantime?

    updateSubmisions(projectKeys, newState);
    release.emptyQueue();

    this.dbUpdateSubmissions(release.getName(), release.getQueue(), projectKeys, newState);
  }

  public void queue(Release nextRelease, List<QueuedProject> queuedProjects) //
      throws InvalidStateException, DccModelOptimisticLockException {
    String nextReleaseName = nextRelease.getName();
    log.info("enqueuing {} for {}", queuedProjects, nextReleaseName);

    // update release object
    SubmissionState expectedState = SubmissionState.NOT_VALIDATED;
    nextRelease.enqueue(queuedProjects);
    for(QueuedProject queuedProject : queuedProjects) {
      String projectKey = queuedProject.getKey();
      Submission submission = fetchAndCheckSubmission(nextRelease, projectKey, expectedState);
      submission.setState(SubmissionState.QUEUED);
    }

    updateRelease(nextReleaseName, nextRelease);
    log.info("enqueued {} for {}", queuedProjects, nextReleaseName);
  }

  /**
   * Attempts to set the given project to VALIDATING.<br>
   * <p>
   * This method is robust enough to handle rare cases like when:<br>
   * - the queue was emptied by an admin in another thread (TODO: complete, this is only partially supported now)<br>
   * - the optimistic lock on Release cannot be obtained (retries a number of time before giving up)<br>
   */
  public void dequeueToValidating(QueuedProject nextProject) {
    String nextProjectKey = nextProject.getKey();
    log.info("Attempting to set {} to validating", nextProjectKey);

    String nextReleaseName = null;
    SubmissionState expectedState = SubmissionState.QUEUED;

    int attempts = 0;
    int MAX_ATTEMPTS = 10; // 10 attempts should be sufficient to obtain a lock (otherwise the problem is probably not
                           // recoverable - deadlock or other)
    while(attempts < MAX_ATTEMPTS) {

      try {
        Release nextRelease = getNextRelease().getRelease();
        nextReleaseName = nextRelease.getName();
        log.info("Dequeuing {} to validating for {}", new Object[] { nextProjectKey, nextReleaseName });

        // actually dequeue the project
        QueuedProject dequeuedProject = nextRelease.dequeueProject();
        String dequeuedProjectKey = dequeuedProject.getKey();
        if(dequeuedProjectKey.equals(nextProjectKey) == false) { // not recoverable: TODO: create dedicated exception?
          throw new ReleaseException("mismatch: " + dequeuedProjectKey + " != " + nextProjectKey);
        }

        // update release object
        Submission submission = getSubmissionByName(nextRelease, nextProjectKey);
        checkNotNull(submission); // checked in getSubmissionByName

        SubmissionState currentState = submission.getState();
        if(expectedState != currentState) {
          throw new ReleaseException( // not recoverable
              "project " + nextProjectKey + " is not " + expectedState + " (" + currentState + " instead)");
        }
        submission.setState(SubmissionState.VALIDATING);

        // update corresponding database entity
        updateRelease(nextReleaseName, nextRelease);

        log.info("Dequeued {} to validating state for {}", nextProjectKey, nextReleaseName);
        break;
      } catch(DccModelOptimisticLockException e) {
        attempts++;
        log.warn(
            "there was a concurrency issue while attempting to set {} to validating state for release {}, number of attempts: {}",
            new Object[] { nextProjectKey, nextReleaseName, attempts });
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS); // TODO: cleanup - use Executor instead?
      }
    }
    if(attempts >= MAX_ATTEMPTS) {
      String message = String.format("failed to validate project %s (could never acquire lock)", nextProjectKey);
      MailUtils.sendEmail(this.config, message, message);
      throw new DccConcurrencyException(message);
    }
  }

  /**
   * Attempts to resolve the given project, if the project is found the given state is set for it.<br>
   * <p>
   * This method is robust enough to handle rare cases like when:<br>
   * - the queue was emptied by an admin in another thread (TODO: complete, this is only partially supported now)<br>
   * - the optimistic lock on Release cannot be obtained (retries a number of time before giving up)<br>
   */
  public void resolve(String projectKey, SubmissionState destinationState) { // TODO: avoid code duplication (see method
                                                                             // above)
    checkArgument(SubmissionState.VALID == destinationState || SubmissionState.INVALID == destinationState
        || SubmissionState.ERROR == destinationState);

    log.info("attempting to resolve {} (as {})", new Object[] { projectKey, destinationState });

    String nextReleaseName = null;
    SubmissionState expectedState = SubmissionState.VALIDATING;

    int attempts = 0;
    int MAX_ATTEMPTS = 10; // 10 attempts should be sufficient to obtain a lock (otherwise the problem is probably not
                           // recoverable - deadlock or other)
    while(attempts < MAX_ATTEMPTS) {

      try {
        Release nextRelease = getNextRelease().getRelease();
        nextReleaseName = nextRelease.getName();
        log.info("resolving {} (as {}) for {}", new Object[] { projectKey, destinationState, nextReleaseName });

        Submission submission = getSubmissionByName(nextRelease, projectKey);
        checkNotNull(submission); // checked in getSubmissionByName

        SubmissionState currentState = submission.getState();
        if(expectedState != currentState) {
          throw new ReleaseException( // not recoverable
              "project " + projectKey + " is not " + expectedState + " (" + currentState + " instead)");
        }
        submission.setState(destinationState);

        // update corresponding database entity
        updateRelease(nextReleaseName, nextRelease);

        log.info("resolved {} for {}", projectKey, nextReleaseName);
        break;
      } catch(DccModelOptimisticLockException e) {
        attempts++;
        log.warn(
            "there was a concurrency issue while attempting to resolve {} (as {}) for release {}, number of attempts: {}",
            new Object[] { projectKey, destinationState, nextReleaseName, attempts });
        Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS); // TODO: cleanup - use Executor instead?
      }
    }
    if(attempts >= MAX_ATTEMPTS) {
      String message = String.format("failed to resolve project %s (could never acquire lock)", projectKey);
      MailUtils.sendEmail(this.config, message, message);
      throw new DccConcurrencyException(message);
    }
  }

  /**
   * Does not allow to update submissions, {@code ProjectService.addProject()} must be used instead
   */
  public Release update(Release updatedRelease) { // This method is not included in NextRelease because of its
                                                  // dependence on methods from NextRelease (we may reconsider in the
                                                  // future) - see comments in DCC-245
    checkArgument(updatedRelease != null);

    String updatedName = updatedRelease.getName();
    String updatedDictionaryVersion = updatedRelease.getDictionaryVersion();

    Release oldRelease = getNextRelease().getRelease();
    String oldName = oldRelease.getName();
    String oldDictionaryVersion = oldRelease.getDictionaryVersion();
    checkState(oldRelease.getState() == ReleaseState.OPENED);

    boolean sameName = oldName.equals(updatedName);
    boolean sameDictionary = oldDictionaryVersion.equals(updatedDictionaryVersion);

    if(!NameValidator.validate(updatedName)) {
      throw new ReleaseException("Updated release name " + updatedName + " is not valid");
    }

    if(sameName == false && getFromName(updatedName) != null) {
      throw new ReleaseException("New release name " + updatedName + " conflicts with an existing release");
    }

    if(updatedDictionaryVersion == null) {
      throw new ReleaseException("Release must have associated dictionary before being updated");
    }

    if(sameDictionary == false && getDictionaryFromVersion(updatedDictionaryVersion) == null) {
      throw new ReleaseException("Release must point to an existing dictionary, no match for "
          + updatedDictionaryVersion);
    }

    // only TWO parameters can be updated for now (though updating the dictionary resets all the submission states)
    oldRelease.setName(updatedName);
    oldRelease.setDictionaryVersion(updatedDictionaryVersion);
    ArrayList<String> oldProjectKeys = Lists.newArrayList(oldRelease.getProjectKeys());
    if(sameDictionary == false) {
      oldRelease.emptyQueue();
      updateSubmisions(oldProjectKeys, SubmissionState.NOT_VALIDATED);
    }

    Query<Release> updateQuery = datastore().createQuery(Release.class)//
        .filter("name = ", oldName);
    UpdateOperations<Release> ops =
        datastore().createUpdateOperations(Release.class).set("name", updatedName)
            .set("dictionaryVersion", updatedDictionaryVersion);
    datastore().update(updateQuery, ops);
    if(sameDictionary == false) {
      dbUpdateSubmissions( // TODO: refactor with redundant code in resetSubmissions()?
          updatedName, oldRelease.getQueue(), oldProjectKeys, SubmissionState.NOT_VALIDATED);
    }

    return oldRelease;
  }

  public void resetSubmissions(final Release release) {
    for(Submission submission : release.getSubmissions()) {
      resetSubmission(release.getName(), submission.getProjectKey());
    }
  }

  public void resetSubmission(final String releaseName, final String projectKey) {
    log.info("resetting submission for project {}", projectKey);
    Release release = datastore().findAndModify( //
        datastore().createQuery(Release.class) //
            .filter("name = ", releaseName) //
            .filter("submissions.projectKey = ", projectKey), //
        datastore().createUpdateOperations(Release.class).disableValidation() //
            .set("submissions.$.state", SubmissionState.NOT_VALIDATED) //
            .unset("submissions.$.report"), false);

    Submission submission = release.getSubmission(projectKey);
    if(submission == null || submission.getState() != SubmissionState.NOT_VALIDATED || submission.getReport() != null) {
      throw new ReleaseException("resetting submission failed for project " + projectKey);
    }
  }

  private Query<Dictionary> buildDictionaryVersionQuery(String dictionaryVersion) {
    return this.datastore().createQuery(Dictionary.class) //
        .filter("version", dictionaryVersion);
  }

  // TODO: should also take care of updating the queue, as the two should always go together
  private void updateSubmisions(List<String> projectKeys, final SubmissionState state) {
    final String releaseName = getNextRelease().getRelease().getName();
    for(String projectKey : projectKeys) {
      getSubmission(releaseName, projectKey).setState(state);
    }
  }

  /**
   * Updates the queue then the submissions states accordingly
   * <p>
   * Always update queue and submission states together (else state may be inconsistent)
   * <p>
   * TODO: should probably revisit all this as it is not very clean
   */
  private void dbUpdateSubmissions(String currentReleaseName, List<QueuedProject> queue, List<String> projectKeys,
      SubmissionState newState) {
    checkArgument(currentReleaseName != null);
    checkArgument(queue != null);

    Query<Release> updateQuery = datastore().createQuery(Release.class)//
        .filter("name = ", currentReleaseName);
    UpdateOperations<Release> ops = datastore().createUpdateOperations(Release.class).disableValidation()//
        .set("queue", queue);
    datastore().update(updateQuery, ops);

    for(String projectKey : projectKeys) {
      updateSubmission(currentReleaseName, newState, projectKey);
    }
  }

  private void updateSubmission(String currentReleaseName, SubmissionState newState, String projectKey) {
    Query<Release> updateQuery;
    UpdateOperations<Release> ops;
    updateQuery = datastore().createQuery(Release.class)//
        .filter("name = ", currentReleaseName)//
        .filter("submissions.projectKey = ", projectKey);
    ops = datastore().createUpdateOperations(Release.class).disableValidation()//
        .set("submissions.$.state", newState);
    datastore().update(updateQuery, ops);
  }

  public void updateSubmission(String currentReleaseName, Submission submission) {
    this.updateSubmission(currentReleaseName, submission.getState(), submission.getProjectKey());
  }

  public void updateSubmissionReport(String releaseName, String projectKey, SubmissionReport report) {
    Query<Release> updateQuery = datastore().createQuery(Release.class)//
        .filter("name = ", releaseName)//
        .filter("submissions.projectKey = ", projectKey);

    UpdateOperations<Release> ops = datastore().createUpdateOperations(Release.class).disableValidation()//
        .set("submissions.$.report", report);

    datastore().update(updateQuery, ops);
  }

  public void removeSubmissionReport(String releaseName, String projectKey) {
    Query<Release> updateQuery = datastore().createQuery(Release.class)//
        .filter("name = ", releaseName)//
        .filter("submissions.projectKey = ", projectKey);

    UpdateOperations<Release> ops =
        datastore().createUpdateOperations(Release.class).disableValidation().unset("submissions.$.report");

    datastore().update(updateQuery, ops);
  }

  public List<Project> getProjects(Release release, Subject user) {
    List<String> projectKeys = new ArrayList<String>();
    for(Submission submission : release.getSubmissions()) {
      if(user.isPermitted(AuthorizationPrivileges.projectViewPrivilege(submission.getProjectKey()))) {
        projectKeys.add(submission.getProjectKey());
      }
    }
    return this.getProjects(projectKeys);
  }

  private List<Project> getProjects(List<String> projectKeys) {
    MorphiaQuery<Project> query = new MorphiaQuery<Project>(morphia(), datastore(), QProject.project);
    return query.where(QProject.project.key.in(projectKeys)).list();
  }

  private Project getProject(String projectKey) {
    MorphiaQuery<Project> query = new MorphiaQuery<Project>(morphia(), datastore(), QProject.project);
    return query.where(QProject.project.key.eq(projectKey)).uniqueResult();
  }

  public List<SubmissionFile> getSubmissionFiles(String releaseName, String projectKey) {
    Release release = this.where(QRelease.release.name.eq(releaseName)).singleResult();
    if(release == null) {
      throw new ReleaseException("No such release");
    }

    Dictionary dict = this.getDictionaryFromVersion(release.getDictionaryVersion());

    if(dict == null) {
      throw new ReleaseException("No Dictionary " + release.getDictionaryVersion());
    }

    List<SubmissionFile> submissionFileList = new ArrayList<SubmissionFile>();
    for(Path path : HadoopUtils.lsFile(this.fs.getFileSystem(), //
        this.fs.buildProjectStringPath(release, projectKey))) { // TODO: use DccFileSystem abstraction instead
      submissionFileList.add(new SubmissionFile(path, fs.getFileSystem(), dict));
    }
    return submissionFileList;
  }

  private Submission fetchAndCheckSubmission(Release nextRelease, String projectKey, SubmissionState expectedState)
      throws InvalidStateException {
    Submission submission = getSubmissionByName(nextRelease, projectKey);
    String errorMessage;
    if(submission == null) {
      errorMessage = "project " + projectKey + " cannot be found";
      log.error(errorMessage);
      throw new InvalidStateException(ServerErrorCode.PROJECT_KEY_NOT_FOUND, errorMessage);
    }
    SubmissionState currentState = submission.getState();
    if(expectedState != currentState) {
      errorMessage = "project " + projectKey + " is not " + expectedState + " (" + currentState + " instead)";
      log.error(errorMessage);
      throw new InvalidStateException(ServerErrorCode.INVALID_STATE, errorMessage);
    }
    return submission;
  }

  private List<String> getSubmission(final SubmissionState state) {
    List<String> projectKeys = new ArrayList<String>();
    List<Submission> submissions = this.getNextRelease().getRelease().getSubmissions();
    for(Submission submission : submissions) {
      if(state.equals(submission.getState())) {
        projectKeys.add(submission.getProjectKey());
      }
    }
    return projectKeys;
  }

  /**
   * Updates the release with the given name, there must be a matching release.<br>
   * 
   * Concurrency is handled with <code>{@link BaseEntity#internalVersion}</code> (optimistic lock).
   * 
   * @throws DccModelOptimisticLockException if optimistic lock fails
   * @throws ReleaseException if the update fails for other reasons (probably not recoverable)
   */
  private void updateRelease(String originalReleaseName, Release updatedRelease) throws DccModelOptimisticLockException {
    UpdateResults<Release> update = null;
    try {
      update = datastore().updateFirst( //
          datastore().createQuery(Release.class) //
              .filter("name = ", originalReleaseName), //
          updatedRelease, false);
    } catch(ConcurrentModificationException e) { // see method comments for why this could be thrown
      log.warn("a possibly recoverable concurrency issue arose when trying to update release {}", originalReleaseName);
      throw new DccModelOptimisticLockException(e);
    }
    if(update == null || update.getHadError()) {
      log.error("an unrecoverable error happenend when trying to update release {}", originalReleaseName);
      throw new ReleaseException(String.format("failed to update release %s", originalReleaseName));
    }
  }

  /**
   * Attempts to retrieve a submission for the given project key provided from the release object provided (no database
   * call).
   * <p>
   * Throws a {@code ReleaseException} if not matching submission is found.
   */
  private Submission getSubmissionByName(Release release, String projectKey) {
    Submission submission = release.getSubmission(projectKey);
    if(submission == null) {
      throw new ReleaseException(String.format("there is no project \"%s\" associated with release \"%s\"", projectKey,
          release.getName()));
    }
    return submission;
  }

  private Dictionary getDictionaryFromVersion(String version) { // also found in DictionaryService - see comments in
                                                                // DCC-245
    return new MorphiaQuery<Dictionary>(morphia(), datastore(), QDictionary.dictionary).where(
        QDictionary.dictionary.version.eq(version)).singleResult();
  }

  private List<Entry<String, String>> buildProjectEntries(List<Project> projects) {
    List<Entry<String, String>> projectEntries = new ArrayList<Map.Entry<String, String>>();
    for(Project project : projects) {
      projectEntries.add(new SimpleEntry<String, String>(project.getKey(), project.getName()));
    }
    return ImmutableList.<Entry<String, String>> copyOf(projectEntries);
  }

  private Map<String, List<SubmissionFile>> buildSubmissionFilesMap(String releaseName, Release release) {
    Map<String, List<SubmissionFile>> submissionFilesMap = Maps.<String, List<SubmissionFile>> newLinkedHashMap();
    for(String projectKey : release.getProjectKeys()) {
      submissionFilesMap.put(projectKey, getSubmissionFiles(releaseName, projectKey));
    }
    return ImmutableMap.<String, List<SubmissionFile>> copyOf(submissionFilesMap);
  }

}
