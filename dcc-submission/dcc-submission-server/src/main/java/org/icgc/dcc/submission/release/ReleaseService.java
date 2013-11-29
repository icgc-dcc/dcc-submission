/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.release;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.icgc.dcc.submission.release.model.Release.SIGNED_OFF_PROJECTS_PREDICATE;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.release.model.SubmissionState.ERROR;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.NOT_VALIDATED;
import static org.icgc.dcc.submission.release.model.SubmissionState.QUEUED;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALIDATING;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Synchronized;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.core.model.BaseEntity;
import org.icgc.dcc.submission.core.model.DccModelOptimisticLockException;
import org.icgc.dcc.submission.core.model.InvalidStateException;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.core.model.QProject;
import org.icgc.dcc.submission.core.morphia.BaseMorphiaService;
import org.icgc.dcc.submission.core.util.NameValidator;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.DictionaryState;
import org.icgc.dcc.submission.dictionary.model.QDictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.fs.SubmissionFile;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
import org.icgc.dcc.submission.release.model.LiteProject;
import org.icgc.dcc.submission.release.model.QRelease;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.ReleaseView;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.shiro.AuthorizationPrivileges;
import org.icgc.dcc.submission.validation.core.SubmissionReport;
import org.icgc.dcc.submission.web.InvalidNameException;
import org.icgc.dcc.submission.web.model.ServerErrorCode;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.UpdateOperations;
import com.google.code.morphia.query.UpdateResults;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.Predicate;

@Slf4j
public class ReleaseService extends BaseMorphiaService<Release> {

  private final DccFileSystem fs;

  /**
   * Temporary solution until DCC-1262 is addressed
   */
  private final ReleaseRepository<Release> releaseRepository;

  @Inject
  public ReleaseService(Morphia morphia, Datastore datastore, @NonNull
  DccFileSystem fs, MailService mailService) {
    super(morphia, datastore, QRelease.release, mailService);
    this.fs = fs;
    this.releaseRepository = new ReleaseRepository<Release>();

    registerModelClasses(Release.class);
  }

  @Synchronized
  public List<String> getQueuedProjectKeys() {
    return getNextRelease().getQueuedProjectKeys();
  }

  @Synchronized
  public Release release(String nextReleaseName) throws InvalidStateException {
    // check for next release name
    if (NameValidator.validateEntityName(nextReleaseName) == false) {
      throw new InvalidNameException(nextReleaseName);
    }

    val oldRelease = getNextRelease();
    Release newRelease = null;
    try {
      String errorMessage;

      if (oldRelease == null) { // just in case (can't really happen)
        errorMessage = "could not acquire lock on release";
        log.error(errorMessage);
        throw new ReleaseException("ReleaseException");
      }
      if (oldRelease.getState() != ReleaseState.OPENED) {
        throw new InvalidStateException(ServerErrorCode.INVALID_STATE, "Release is not open");
      }
      if (isAtLeastOneSignedOff(oldRelease) == false) { // check for signed-off submission states (must have at least
                                                        // one)
        errorMessage = "no signed off project in " + oldRelease;
        log.error(errorMessage);
        throw new InvalidStateException(ServerErrorCode.SIGNED_OFF_SUBMISSION_REQUIRED, errorMessage);
      }
      if (oldRelease.getQueue().isEmpty() == false) {
        errorMessage = "some projects are still enqueue in " + oldRelease;
        log.error(errorMessage);
        throw new InvalidStateException(ServerErrorCode.QUEUE_NOT_EMPTY, errorMessage);
      }

      val dictionaryVersion = oldRelease.getDictionaryVersion();
      if (dictionaryVersion == null) {
        errorMessage = "could not find a dictionary matching null";
        log.error(errorMessage);
        throw new InvalidStateException(ServerErrorCode.RELEASE_MISSING_DICTIONARY, errorMessage); // TODO: new kind of
                                                                                                   // exception rather?
      }
      if (getReleaseByName(nextReleaseName) != null) {
        errorMessage = "found a conflicting release for name " + nextReleaseName;
        log.error(errorMessage);
        throw new InvalidStateException(ServerErrorCode.DUPLICATE_RELEASE_NAME, errorMessage);
      }

      newRelease = doRelease(oldRelease, nextReleaseName, dictionaryVersion);
    } catch (RuntimeException e) {
      throw new ReleaseException("Exception trying to release", e);
    }

    return newRelease;
  }

  private Release doRelease(
      @NonNull
      Release oldRelease,
      @NonNull
      String nextReleaseName,
      @NonNull
      String dictionaryVersion) {

    // Create new release entity
    val newRelease = createNextRelease(oldRelease, nextReleaseName, dictionaryVersion);

    // Set up new release file system counterpart
    setUpNewReleaseFileSystem(oldRelease, newRelease);

    // Must happen AFTER creating the new release object and setting up the file system (both operations need the old
    // release in its pre-completion state)
    log.info("Completing old release entity object: '{}'", oldRelease.getName());
    oldRelease.complete();

    // Persist modified entity objects
    log.info("Persisting changes");
    releaseRepository.closeDictionary(dictionaryVersion);
    releaseRepository.updateCompletedRelease(oldRelease);
    releaseRepository.saveNewRelease(newRelease);

    return newRelease;
  }

  private Release createNextRelease(
      @NonNull
      Release oldRelease,
      @NonNull
      String name,
      @NonNull
      String dictionaryVersion) {
    val nextRelease = new Release(name);
    nextRelease.setDictionaryVersion(dictionaryVersion);
    nextRelease.setState(ReleaseState.OPENED);

    for (val submission : oldRelease.getSubmissions()) {
      val newSubmission =
          new Submission(submission.getProjectKey(), submission.getProjectName(), nextRelease.getName());
      if (submission.getState() == SubmissionState.SIGNED_OFF) {
        newSubmission.setState(SubmissionState.NOT_VALIDATED);
      } else {
        newSubmission.setState(submission.getState());
        newSubmission.setReport(submission.getReport());
      }
      nextRelease.addSubmission(newSubmission);
    }

    return nextRelease;
  }

  private void setUpNewReleaseFileSystem(Release oldRelease, Release nextRelease) {
    fs.getReleaseFilesystem(nextRelease)
        .setUpNewReleaseFileSystem(
            oldRelease.getName(), // Remove after DCC-1940
            nextRelease.getName(),

            // The release file system
            fs.getReleaseFilesystem(oldRelease),

            // The signed off projects
            extractProjectKeys(filter(oldRelease.getSubmissions(), SIGNED_OFF_PROJECTS_PREDICATE)),

            // The remaining projects
            extractProjectKeys(filter(oldRelease.getSubmissions(), not(SIGNED_OFF_PROJECTS_PREDICATE))));
  }

  boolean isAtLeastOneSignedOff(Release release) {
    for (val submission : release.getSubmissions()) {
      if (submission.getState() == SubmissionState.SIGNED_OFF) {
        return true;
      }
    }
    return false;
  }

  public Release getReleaseByName(String releaseName) {
    return where(QRelease.release.name.eq(releaseName)).uniqueResult();
  }

  /**
   * Returns a list of {@code Release}s with their @{code Submission} filtered based on the user's privilege on
   * projects.
   */
  @Synchronized
  public List<Release> getReleasesBySubject(Subject subject) {
    log.debug("getting releases for {}", subject.getPrincipal());

    List<Release> releases = releaseRepository.listReleases();
    log.debug("#releases: ", releases.size());

    // filter out all the submissions that the current user can not see
    for (val release : releases) {
      List<Submission> newSubmissions = Lists.newArrayList();
      for (val submission : release.getSubmissions()) {
        String projectKey = submission.getProjectKey();
        if (subject.isPermitted(AuthorizationPrivileges.projectViewPrivilege(projectKey))) {
          newSubmissions.add(submission);
        }
      }

      release.getSubmissions().clear(); // TODO: should we manipulate release this way? consider creating DTO?
      release.getSubmissions().addAll(newSubmissions);
    }

    log.debug("#releases visible: ", releases.size());
    return releases;
  }

  @Synchronized
  public void createInitialRelease(Release initRelease) {
    // check for init release name
    if (!NameValidator.validateEntityName(initRelease.getName())) {
      throw new InvalidNameException(initRelease.getName());
    }

    val dictionaryVersion = initRelease.getDictionaryVersion();
    log.info("Dictionary version used: '{}'", dictionaryVersion);

    if (dictionaryVersion == null) {
      throw new ReleaseException("Dictionary version must not be null!");
    } else if (releaseRepository.getDictionaryForVersion(dictionaryVersion) == null) {
      throw new ReleaseException("Specified dictionary version not found in DB: " + dictionaryVersion);
    }

    // Just use name and dictionaryVersion from incoming json
    val nextRelease = new Release(initRelease.getName());
    nextRelease.setDictionaryVersion(dictionaryVersion);
    releaseRepository.saveNewRelease(nextRelease);

    // after initial release, create initial file system
    Set<String> projects = Sets.newHashSet();
    fs.createInitialReleaseFilesystem(nextRelease, projects);
  }

  /**
   * Returns the number of releases that are in the {@link ReleaseState#OPENED} state. It is expected that there always
   * ever be one at a time.
   */
  @Synchronized
  public long countOpenReleases() {
    return query()
        .where(
            QRelease.release.state.eq(OPENED))
        .count();
  }

  /**
   * Optionally returns a {@code ReleaseView} matching the given name, and for which {@code Submission}s are filtered
   * based on the user's privileges.
   */
  public Optional<ReleaseView> getReleaseViewBySubject(String releaseName, Subject user) {
    val release = where(QRelease.release.name.eq(releaseName)).uniqueResult();
    Optional<ReleaseView> releaseView = Optional.absent();
    if (release != null) {
      // populate project name for submissions
      val projects = getProjects(release, user);
      val liteProjects = buildLiteProjects(projects);
      val submissionFilesMap = buildSubmissionFilesMap(releaseName, release);
      releaseView = Optional.of(new ReleaseView(release, liteProjects, submissionFilesMap));
    }

    return releaseView;
  }

  /**
   * Returns the {@code NextRelease} (guaranteed not to be null if returned).
   */
  @Synchronized
  public Release getNextRelease() throws IllegalReleaseStateException {
    val nextRelease = where(
        QRelease.release.state.eq(OPENED))
        .singleResult();

    checkNotNull(nextRelease, "There is no next release in the database.");

    return nextRelease;
  }

  /**
   * Returns the current dictionary.
   * <p>
   * This is the dictionary, open or not, that the {@code NextRelease}'s {@code Release} points to.
   */
  public Dictionary getNextDictionary() {
    val release = getNextRelease();
    val version = release.getDictionaryVersion();
    return releaseRepository.getDictionaryFromVersion(version);
  }

  /**
   * Returns a non-null list of {@code Release} (possibly empty)
   */
  public List<Release> list() {
    return releaseRepository.listReleases();
  }

  public Release getCompletedRelease(String releaseName) throws IllegalReleaseStateException {
    val query = where(QRelease.release.state.eq(ReleaseState.COMPLETED).and(QRelease.release.name.eq(releaseName)));
    val release = query.uniqueResult();
    if (release == null) {
      throw new IllegalArgumentException("release " + releaseName + " is not complete");
    }

    return release;
  }

  public Submission getSubmission(String releaseName, String projectKey) {
    val release = where(QRelease.release.name.eq(releaseName)).uniqueResult();
    checkArgument(release != null);

    return getSubmission(release, projectKey);
  }

  public DetailedSubmission getDetailedSubmission(String releaseName, String projectKey) {
    val submission = getSubmission(releaseName, projectKey);
    val liteProject = new LiteProject(checkNotNull(getProject(projectKey)));

    val detailedSubmission = new DetailedSubmission(submission, liteProject);
    detailedSubmission.setSubmissionFiles(getSubmissionFiles(releaseName, projectKey));

    return detailedSubmission;
  }

  public List<Release> getCompletedReleases() throws IllegalReleaseStateException {
    val completedReleases = new ArrayList<Release>();
    for (val release : releaseRepository.listReleases(Optional.<Predicate> of(QRelease.release.state
        .eq(ReleaseState.COMPLETED)))) {
      completedReleases.add(release);
    }

    return completedReleases;
  }

  private Submission getSubmission(Release release, String projectKey) {
    for (val submission : release.getSubmissions()) {
      if (submission.getProjectKey().equals(projectKey)) {
        return submission;
      }
    }

    throw new ReleaseException(String.format("there is no project \"%s\" associated with release \"%s\"", projectKey,
        release.getName()));
  }

  public List<String> getSignedOff() {
    return getSubmission(SubmissionState.SIGNED_OFF);
  }

  @Synchronized
  public void signOff(Release nextRelease, List<String> projectKeys, String user)
      throws InvalidStateException, DccModelOptimisticLockException {

    String nextReleaseName = nextRelease.getName();
    log.info("signing off {} for {}", projectKeys, nextReleaseName);

    // update release object
    val expectedState = SubmissionState.VALID;
    nextRelease.removeFromQueue(projectKeys);
    for (val projectKey : projectKeys) {
      Submission submission = fetchAndCheckSubmission(nextRelease, projectKey, expectedState);
      submission.setState(SubmissionState.SIGNED_OFF);
    }

    updateRelease(nextReleaseName, nextRelease);

    // TODO: synchronization (DCC-685), may require cleaning up the FS abstraction (do we really need the project object
    // or is the projectKey sufficient?)
    // remove .validation folder from the Submission folder
    val releaseFs = fs.getReleaseFilesystem(nextRelease);
    val projects = getProjects(projectKeys);
    for (val project : projects) {
      SubmissionDirectory submissionDirectory = releaseFs.getSubmissionDirectory(project.getKey());
      submissionDirectory.removeValidationDir();
    }

    // after sign off, send a email to DCC support
    mailService.sendSignoff(user, projectKeys, nextReleaseName);

    log.info("signed off {} for {}", projectKeys, nextReleaseName);
  }

  @Synchronized
  public void deleteQueuedRequests() {
    log.info("emptying queue");

    val newState = NOT_VALIDATED;
    val release = getNextRelease();
    val projectKeys = release.getQueuedProjectKeys(); // TODO: what if nextrelease changes in the meantime?

    // FIXME: DCC-901
    updateSubmisions(projectKeys, newState);
    release.emptyQueue();

    dbUpdateSubmissions(release.getName(), release.getQueue(), projectKeys, newState); // FIXME: DCC-901
    for (String projectKey : projectKeys) {
      // See spec at https://wiki.oicr.on.ca/display/DCCSOFT/Concurrency#Concurrency-Submissionstatesresetting
      resetValidationFolder(projectKey, release);
    }
  }

  @Synchronized
  public void deleteQueuedRequest(String projectKey) throws InvalidStateException {
    log.info("Deleting queued request for project '{}'", projectKey);

    val release = getNextRelease();
    val projectKeys = singletonList(projectKey);
    val state = getSubmission(release, projectKey).getState();
    val queued = state == QUEUED;
    val validating = state == VALIDATING;
    val active = validating || queued;
    log.info("Submission state for '{}' when delete queue request called is '{}'", projectKey, state);

    if (queued) {
      log.info("Removing project form queue: {}", projectKey);
      release.removeFromQueue(projectKey);
    }

    if (active) {
      val newState = NOT_VALIDATED;
      log.info("Updating in-memory '{}' project submission state to '{}'", projectKey, newState);
      updateSubmisions(projectKeys, newState);

      log.info("Updating database '{}' release queue to '{}' and project '{}' submission state to '{}'",
          new Object[] { release.getName(), release.getQueue(), projectKey, newState });
      dbUpdateSubmissions(release.getName(), release.getQueue(), projectKeys, newState);

      log.info("Resetting file system '{}' project validation folder", projectKey);
      resetValidationFolder(projectKey, release);
    }
  }

  @Synchronized
  public void queue(Release nextRelease, List<QueuedProject> queuedProjects)
      throws InvalidStateException, DccModelOptimisticLockException {
    val nextReleaseName = nextRelease.getName();
    log.info("enqueuing {} for {}", queuedProjects, nextReleaseName);

    // Update release object
    val expectedState = NOT_VALIDATED;
    nextRelease.enqueue(queuedProjects);

    for (val queuedProject : queuedProjects) {
      val projectKey = queuedProject.getKey();
      Submission submission = fetchAndCheckSubmission(nextRelease, projectKey, expectedState);
      submission.setState(QUEUED);
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
  @Synchronized
  public void dequeueToValidating(QueuedProject nextProject) {
    val expectedState = SubmissionState.QUEUED;
    val nextProjectKey = nextProject.getKey();

    val description = format("validate project '%s'", nextProjectKey);
    log.info("Attempting to {}", description);

    withRetry(description, new Callable<Optional<?>>() {

      @Override
      public Optional<?> call() throws DccModelOptimisticLockException {
        val nextRelease = getNextRelease();
        val nextReleaseName = nextRelease.getName();

        log.info("Dequeuing {} to validating for {}", nextProjectKey, nextReleaseName);

        // actually dequeue the project
        val dequeuedProject = nextRelease.dequeueProject();
        val dequeuedProjectKey = dequeuedProject.getKey();
        if (dequeuedProjectKey.equals(nextProjectKey) == false) { // not recoverable: TODO: create dedicated exception?
          throw new ReleaseException("Mismatch: " + dequeuedProjectKey + " != " + nextProjectKey);
        }

        // update release object
        val submission = getSubmissionByName(nextRelease, nextProjectKey); // can't be null
        SubmissionState currentState = submission.getState();
        SubmissionState destinationState = SubmissionState.VALIDATING;
        if (expectedState != currentState) {
          throw new ReleaseException( // not recoverable
              "Project " + nextProjectKey + " is not " + expectedState + " (" + currentState
                  + " instead), cannot set to " + destinationState);
        }
        submission.setState(destinationState);

        // update corresponding database entity
        updateRelease(nextReleaseName, nextRelease);

        log.info("Dequeued {} to validating state for {}", nextProjectKey, nextReleaseName);
        return Optional.absent();
      }

    });
  }

  /**
   * Attempts to resolve the given project, if the project is found the given state is set for it.<br>
   * <p>
   * This method is robust enough to handle rare cases like when:<br>
   * - the queue was emptied by an admin in another thread (TODO: complete, this is only partially supported now)<br>
   * - the optimistic lock on Release cannot be obtained (retries a number of time before giving up)<br>
   */
  @Synchronized
  public void resolve(final String projectKey, final SubmissionState destinationState) { // TODO: avoid code duplication
    checkArgument(
    /**/VALID == destinationState ||
        INVALID == destinationState ||
        ERROR == destinationState ||
        NOT_VALIDATED == destinationState /* Cancelled */);

    val expectedState = VALIDATING;

    String description = format("resolve project '%s' with destination state '%s')", projectKey, destinationState);
    log.info("Attempting to {}", description);

    withRetry(description, new Callable<Optional<?>>() {

      @Override
      public Optional<?> call() throws DccModelOptimisticLockException {
        val nextRelease = getNextRelease();
        String nextReleaseName = nextRelease.getName();

        log.info("Resolving {} (as {}) for {}", new Object[] { projectKey, destinationState, nextReleaseName });

        val submission = getSubmissionByName(nextRelease, projectKey);
        val currentState = submission.getState();
        if (expectedState != currentState) {
          throw new ReleaseException("project " + projectKey + " is not " + expectedState + " (" + currentState
              + " instead), cannot set to " + destinationState);
        }

        // Update corresponding database entity
        updateSubmissionState(nextReleaseName, projectKey, destinationState);

        log.info("Resolved {} for {}", projectKey, nextReleaseName);
        return Optional.absent();
      }

    });
  }

  /**
   * Updates a release name and/or dictionary version.
   * <p>
   * Does not allow to update submissions per se, {@code ProjectService.addProject()} must be used instead.
   * <p>
   * This MUST reset submission states.
   * <p>
   * This method is not included in NextRelease because of its dependence on methods from NextRelease (we may reconsider
   * in the future) - see comments in DCC-245
   */
  @Synchronized
  public Release update(String newReleaseName, String newDictionaryVersion) {
    val release = getNextRelease();
    String oldReleaseName = release.getName();
    String oldDictionaryVersion = release.getDictionaryVersion();
    checkState(release.getState() == ReleaseState.OPENED);

    boolean sameName = oldReleaseName.equals(newReleaseName);
    boolean sameDictionary = oldDictionaryVersion.equals(newDictionaryVersion);

    if (!NameValidator.validateEntityName(newReleaseName)) {
      throw new ReleaseException("Updated release name " + newReleaseName + " is not valid");
    }

    if (sameName == false && getReleaseByName(newReleaseName) != null) {
      throw new ReleaseException("New release name " + newReleaseName + " conflicts with an existing release");
    }

    if (newDictionaryVersion == null) {
      throw new ReleaseException("Release must have associated dictionary before being updated");
    }

    if (sameDictionary == false && releaseRepository.getDictionaryFromVersion(newDictionaryVersion) == null) {
      throw new ReleaseException("Release must point to an existing dictionary, no match for " + newDictionaryVersion);
    }

    // Update release object and database entity (top-level entity only)
    log.info("Updating release {} with {} and {}" + (sameDictionary ? " and emptying queue" : ""),
        new Object[] { oldReleaseName, newReleaseName, newDictionaryVersion });
    release.setName(newReleaseName);
    release.setDictionaryVersion(newDictionaryVersion);
    if (sameDictionary == false) {
      release.emptyQueue();
    }

    val releaseUpdate = datastore().update(
        datastore().createQuery(Release.class)
            .filter("name = ", oldReleaseName),
        datastore().createUpdateOperations(Release.class)
            .set("name", newReleaseName)
            .set("dictionaryVersion", newDictionaryVersion)
            .set("queue", release.getQueue()));

    if (releaseUpdate.getUpdatedCount() != 1) { // Ensure update was successful
      notifyUpdateError(oldReleaseName, on(",").join(newReleaseName, newDictionaryVersion, release.getQueue()));
    }

    // If a new dictionary was specified, reset submissions, TODO: use resetSubmission() instead (DCC-901)!
    if (sameDictionary == false) {
      // Reset all projects
      resetSubmissions(newReleaseName, release.getProjectKeys());
    }

    return release;
  }

  @Synchronized
  public void resetSubmissions(final String releaseName, final Iterable<String> projectKeys) {
    for (val projectKey : projectKeys) {
      resetSubmission(releaseName, projectKey);
    }
  }

  /**
   * Resets submission to NOT_VALIDATED and empty report.
   * <p>
   * Note that one must also empty the .validation directory for cascading to re-run fully.
   * <p>
   * see DCC-901
   */
  @Synchronized
  public void resetSubmission(final String releaseName, final String projectKey) {
    log.info("Resetting submission for project '{}'", projectKey);

    // Reset state and report in database (TODO: queue + currently validating? - DCC-906)
    // TODO: move to repo
    val release = datastore().findAndModify(
        datastore().createQuery(Release.class)
            .filter("name = ", releaseName)
            .filter("submissions.projectKey = ", projectKey),
        releaseRepository.allowDollarSignForMorphiaUpdatesBug(datastore().createUpdateOperations(Release.class))
            .set("submissions.$.state", SubmissionState.NOT_VALIDATED)
            .unset("submissions.$.report"), false);

    val submission = release.getSubmission(projectKey);
    if (submission == null || submission.getState() != SubmissionState.NOT_VALIDATED || submission.getReport() != null) {
      // TODO: DCC-902 (optimistic lock potential problem: what if this actually happens? - add a retry?)
      log.error("If you see this, then DCC-902 MUST be addressed.");
      throw new ReleaseException("Resetting submission failed for project " + projectKey);
    }

    // Empty .validation dir else cascade may not rerun
    resetValidationFolder(projectKey, release); // TODO: see note in method javadoc
  }

  /**
   * TODO: only taken out of resetSubmission() until DCC-901 is done (to allow code that calls deprecated methods
   * instead of resetSubmission() to still be able to empty those directories)
   */
  @Synchronized
  public void resetValidationFolder(String projectKey, Release release) {
    log.info("Resetting validation folder for '{}' in release '{}'", projectKey, release.getName());
    fs.getReleaseFilesystem(release).resetValidationFolder(projectKey);
  }

  /**
   * TODO: should also take care of updating the queue, as the two should always go together
   * <p>
   * TODO: Isn't this a no-op?!?!
   * <p>
   * deprecation: see DCC-901
   */
  @Deprecated
  private void updateSubmisions(List<String> projectKeys, final SubmissionState state) {
    val releaseName = getNextRelease().getName();
    for (val projectKey : projectKeys) {
      getSubmission(releaseName, projectKey).setState(state);
    }
  }

  /**
   * Updates the queue then the submissions states accordingly
   * <p>
   * Always update queue and submission states together (else state may be inconsistent)
   * <p>
   * TODO: should probably revisit all this as it is not very clean
   * <p>
   * deprecation: see DCC-901
   */
  @Deprecated
  private void dbUpdateSubmissions(String currentReleaseName, List<QueuedProject> queue, List<String> projectKeys,
      SubmissionState newState) {
    checkArgument(currentReleaseName != null);
    checkArgument(queue != null);

    datastore().update(
        datastore().createQuery(Release.class)
            .filter("name = ", currentReleaseName),
        datastore().createUpdateOperations(Release.class)
            .set("queue", queue));

    for (val projectKey : projectKeys) {
      updateSubmission(currentReleaseName, newState, projectKey);
    }
  }

  private void updateSubmission(String currentReleaseName, SubmissionState newState, String projectKey) {
    datastore().update(
        datastore().createQuery(Release.class)
            .filter("name = ", currentReleaseName)
            .filter("submissions.projectKey = ", projectKey),
        releaseRepository.allowDollarSignForMorphiaUpdatesBug(
            datastore().createUpdateOperations(Release.class))
            .set("submissions.$.state", newState));
  }

  @Synchronized
  public void updateSubmission(String currentReleaseName, Submission submission) {
    updateSubmission(currentReleaseName, submission.getState(), submission.getProjectKey());
  }

  @Synchronized
  public void updateSubmissionReport(String releaseName, String projectKey, SubmissionReport report) {
    val update = datastore().update(
        datastore().createQuery(Release.class)
            .filter("name = ", releaseName)
            .filter("submissions.projectKey = ", projectKey),
        releaseRepository.allowDollarSignForMorphiaUpdatesBug(
            datastore().createUpdateOperations(Release.class))
            .set("submissions.$.report", report));

    int updatedCount = update.getUpdatedCount();
    if (updatedCount != 1) { // Only to help diagnosis for now, we're unsure when that happens (DCC-848)
      log.error("Setting submission report containing {} schema reports failed for project '{}'",
          report == null ? null : report.getSchemaReports().size(), releaseName);
    }
  }

  @Synchronized
  public void updateSubmissionState(String releaseName, String projectKey, SubmissionState state) {
    val update = datastore().update(
        datastore().createQuery(Release.class)
            .filter("name = ", releaseName)
            .filter("submissions.projectKey = ", projectKey),
        releaseRepository.allowDollarSignForMorphiaUpdatesBug(
            datastore().createUpdateOperations(Release.class))
            .set("submissions.$.state", state));

    int updatedCount = update.getUpdatedCount();
    if (updatedCount != 1) {
      log.error("Setting submission state to '{}' failed for project '{}'", state, projectKey);
    }
  }

  private List<Project> getProjects(Release release, Subject user) {
    List<String> projectKeys = new ArrayList<String>();
    for (val submission : release.getSubmissions()) {
      if (user.isPermitted(AuthorizationPrivileges.projectViewPrivilege(submission.getProjectKey()))) {
        projectKeys.add(submission.getProjectKey());
      }
    }

    return getProjects(projectKeys);
  }

  private List<Project> getProjects(List<String> projectKeys) {
    val query = new MorphiaQuery<Project>(morphia(), datastore(), QProject.project);
    return query.where(QProject.project.key.in(projectKeys)).list();
  }

  private Project getProject(String projectKey) {
    val query = new MorphiaQuery<Project>(morphia(), datastore(), QProject.project);
    return query.where(QProject.project.key.eq(projectKey)).uniqueResult();
  }

  public List<SubmissionFile> getSubmissionFiles(String releaseName, String projectKey) {
    val release = where(QRelease.release.name.eq(releaseName)).singleResult();
    if (release == null) {
      throw new ReleaseException("No such release");
    }

    val dictionary = releaseRepository.getDictionaryFromVersion(release.getDictionaryVersion());
    if (dictionary == null) {
      throw new ReleaseException("No Dictionary " + release.getDictionaryVersion());
    }

    List<SubmissionFile> submissionFileList = new ArrayList<SubmissionFile>();
    val buildProjectStringPath = new Path(fs.buildProjectStringPath(release.getName(), projectKey));

    for (val path : HadoopUtils.lsFile(fs.getFileSystem(), buildProjectStringPath)) {
      // TODO: use DccFileSystem sabstraction instead
      submissionFileList.add(new SubmissionFile(path, fs.getFileSystem(), dictionary));
    }
    return submissionFileList;
  }

  private Submission fetchAndCheckSubmission(Release nextRelease, String projectKey, SubmissionState expectedState)
      throws InvalidStateException {
    val submission = getSubmissionByName(nextRelease, projectKey);
    String errorMessage;
    if (submission == null) {
      errorMessage = "project " + projectKey + " cannot be found";
      log.error(errorMessage);
      throw new InvalidStateException(ServerErrorCode.PROJECT_KEY_NOT_FOUND, errorMessage);
    }
    val currentState = submission.getState();
    if (expectedState != currentState) {
      errorMessage = "project " + projectKey + " is not " + expectedState + " (" + currentState + " instead)";
      log.error(errorMessage);
      throw new InvalidStateException(ServerErrorCode.INVALID_STATE, errorMessage, currentState);
    }
    return submission;
  }

  private List<String> getSubmission(final SubmissionState state) {
    List<String> projectKeys = new ArrayList<String>();
    List<Submission> submissions = getNextRelease().getSubmissions();
    for (val submission : submissions) {
      if (state.equals(submission.getState())) {
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
      update = datastore().updateFirst(
          datastore().createQuery(Release.class)
              .filter("name = ", originalReleaseName),
          updatedRelease, false);
    } catch (ConcurrentModificationException e) { // see method comments for why this could be thrown
      log.warn("a possibly recoverable concurrency issue arose when trying to update release {}", originalReleaseName);
      throw new DccModelOptimisticLockException(e);
    }
    if (update == null || update.getHadError()) {
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
    val submission = release.getSubmission(projectKey);
    if (submission == null) {
      throw new ReleaseException(String.format("there is no project \"%s\" associated with release \"%s\"", projectKey,
          release.getName()));
    }

    return submission;
  }

  private List<LiteProject> buildLiteProjects(List<Project> projects) {
    List<LiteProject> liteProjects = newArrayList();
    for (val project : projects) {
      liteProjects.add(new LiteProject(project));
    }

    return copyOf(liteProjects);
  }

  private Map<String, List<SubmissionFile>> buildSubmissionFilesMap(String releaseName, Release release) {
    Map<String, List<SubmissionFile>> submissionFilesMap = Maps.newLinkedHashMap();
    for (val projectKey : release.getProjectKeys()) {
      submissionFilesMap.put(projectKey, getSubmissionFiles(releaseName, projectKey));
    }

    return ImmutableMap.copyOf(submissionFilesMap);
  }

  private void notifyUpdateError(String filter, String setValues) {
    notifyUpdateError(filter, setValues, null);
  }

  /**
   * To notify us that an update failed.
   */
  private void notifyUpdateError(String filter, String setValues, String unsetValues) {
    val id = System.currentTimeMillis();
    log.error("Unable to update the release (id: " + id + ") (maybe a lock problem)?", new IllegalStateException());

    String message = format("filter: %s, set values: %s, unset values: %s, id: %s", filter, setValues, unsetValues, id);
    mailService.sendSupportProblem("Automatic email - Failure update", message);
  }

  private Iterable<String> extractProjectKeys(Iterable<Submission> submissions) {
    return transform(
        submissions,
        new Function<Submission, String>() {

          @Override
          public String apply(Submission submission) {
            return submission.getProjectKey();
          }
        });
  }

  /**
   * See comment on {@link ReleaseService#releaseRepository}.
   */
  @NoArgsConstructor(access = AccessLevel.PRIVATE)
  private final class ReleaseRepository<T> {

    /**
     * Idempotent.
     */
    private void closeDictionary(final String oldDictionaryVersion) { // TODO: move to dictionary service?
      log.info("Closing dictionary: '{}'", oldDictionaryVersion);
      datastore().findAndModify(
          datastore().createQuery(Dictionary.class)
              .filter("version", oldDictionaryVersion),
          datastore().createUpdateOperations(Dictionary.class)
              .set("state", DictionaryState.CLOSED));
    }

    private void updateCompletedRelease(
        @NonNull
        Release oldRelease) {
      log.info("Updating completed release: '{}'", oldRelease.getName());
      datastore().findAndModify(
          datastore().createQuery(Release.class)
              .filter("name", oldRelease.getName()),
          datastore().createUpdateOperations(Release.class)
              .set("state", oldRelease.getState())
              .set("releaseDate", oldRelease.getReleaseDate())
              .set("submissions", oldRelease.getSubmissions()));
    }

    /**
     * Do *not* use to update an existing release (not intended that way).
     */
    private void saveNewRelease(
        @NonNull
        Release newRelease) {
      log.info("Saving new release: '{}'", newRelease.getName());
      datastore().save(newRelease);
    }

    private List<Release> listReleases() {
      return listReleases(Optional.<Predicate> absent());
    }

    /**
     * Ignoring releases whose name is starting with a specific prefix (see https://jira.oicr.on.ca/browse/DCC-1409 for
     * more details).
     */
    private List<Release> listReleases(Optional<Predicate> mysemaPredicate) {
      MongodbQuery<Release> query = mysemaPredicate.isPresent() ?
          where(mysemaPredicate.get()) :
          query();
      return query.list();
    }

    // TODO: figure out difference with method below
    private Dictionary getDictionaryFromVersion(@NonNull
    String version) {
      // Also found in DictionaryService - see comments in DCC-245
      return new MorphiaQuery<Dictionary>(morphia(), datastore(), QDictionary.dictionary).where(
          QDictionary.dictionary.version.eq(version)).singleResult();
    }

    private Dictionary getDictionaryForVersion(String dictionaryVersion) {
      return datastore().createQuery(Dictionary.class)
          .filter("version", dictionaryVersion).get();
    }

    /**
     * This is currently necessary in order to use the <i>field.$.nestedField</i> notation in updates. Otherwise one
     * gets an error like <i>
     * "The field '$' could not be found in 'org.icgc.dcc.submission.release.model.Release' while validating - submissions.$.state; if you wish to continue please disable validation."
     * </i>
     * <p>
     * For more information, see
     * http://groups.google.com/group/morphia/tree/browse_frm/month/2011-01/489d5b7501760724?rnum
     * =31&_done=/group/morphia/browse_frm/month/2011-01?
     */
    private UpdateOperations<Release> allowDollarSignForMorphiaUpdatesBug(
        UpdateOperations<Release> updateOperations) {
      return updateOperations.disableValidation();
    }
  }
}
