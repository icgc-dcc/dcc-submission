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
package org.icgc.dcc.submission.service;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.ArrayUtils.contains;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.lsFile;
import static org.icgc.dcc.submission.core.util.NameValidator.validateEntityName;
import static org.icgc.dcc.submission.release.model.Release.SIGNED_OFF_PROJECTS_PREDICATE;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.release.model.Submission.getProjectKeys;
import static org.icgc.dcc.submission.release.model.SubmissionState.ERROR;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.NOT_VALIDATED;
import static org.icgc.dcc.submission.release.model.SubmissionState.QUEUED;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALIDATING;
import static org.icgc.dcc.submission.shiro.AuthorizationPrivileges.projectViewPrivilege;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.DUPLICATE_RELEASE_NAME;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.INVALID_STATE;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.QUEUE_NOT_EMPTY;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.RELEASE_MISSING_DICTIONARY;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.SIGNED_OFF_SUBMISSION_REQUIRED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import lombok.NonNull;
import lombok.Synchronized;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.core.model.BaseEntity;
import org.icgc.dcc.submission.core.model.DccModelOptimisticLockException;
import org.icgc.dcc.submission.core.model.InvalidStateException;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.core.model.SubmissionFile;
import org.icgc.dcc.submission.core.util.NameValidator;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
import org.icgc.dcc.submission.release.model.LiteProject;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.ReleaseView;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.repository.DictionaryRepository;
import org.icgc.dcc.submission.repository.ProjectRepository;
import org.icgc.dcc.submission.repository.ReleaseRepository;
import org.icgc.dcc.submission.validation.ValidationOutcome;
import org.icgc.dcc.submission.validation.core.SubmissionReport;
import org.icgc.dcc.submission.web.InvalidNameException;
import org.icgc.dcc.submission.web.model.ServerErrorCode;
import org.mongodb.morphia.query.UpdateResults;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

@Slf4j
public class ReleaseService extends AbstractService {

  private final SubmissionService submissionService;
  private final DccFileSystem dccFileSystem;
  private final ReleaseRepository releaseRepository;
  private final DictionaryRepository dictionaryRepository;
  private final ProjectRepository projectRepository;

  @Inject
  public ReleaseService(
      @NonNull SubmissionService submissionService,
      @NonNull MailService mailService,
      @NonNull DccFileSystem dccFileSystem,
      @NonNull ReleaseRepository releaseRepository,
      @NonNull DictionaryRepository dictionaryRepository,
      @NonNull ProjectRepository projectRepository) {
    super(mailService);
    this.dccFileSystem = dccFileSystem;
    this.submissionService = submissionService;
    this.releaseRepository = releaseRepository;
    this.dictionaryRepository = dictionaryRepository;
    this.projectRepository = projectRepository;
  }

  /**
   * Returns the number of releases.
   */
  @Synchronized
  public long countReleases() {
    return releaseRepository.countReleases();
  }

  /**
   * Returns the number of releases that are in the {@link ReleaseState#OPENED} state. It is expected that there always
   * ever be one at a time.
   */
  @Synchronized
  public long countOpenReleases() {
    return releaseRepository.countOpenReleases();
  }

  public List<Release> getReleases() {
    log.info("Request to find all Releases");
    return releaseRepository.findReleases();
  }

  /**
   * Returns a list of {@code Release}s with their @{code Submission} filtered based on the user's privilege on
   * projects.
   */
  @Synchronized
  public List<Release> getReleasesBySubject(Subject subject) {
    log.debug("getting releases for {}", subject.getPrincipal());

    List<Release> releases = releaseRepository.findReleases();
    log.debug("Number of releases:{} ", releases.size());

    // Filter out all the submissions that the current user can not see
    for (val release : releases) {
      val builder = ImmutableList.<Submission> builder();
      for (val submission : release.getSubmissions()) {
        val privilege = projectViewPrivilege(submission.getProjectKey());
        val permitted = subject.isPermitted(privilege);
        if (permitted) {
          builder.add(submission);
        }
      }

      val submissions = release.getSubmissions();
      submissions.clear(); // TODO: should we manipulate release this way? consider creating DTO?
      submissions.addAll(builder.build());
    }

    log.debug("Number of releases visible: {}", releases.size());
    return releases;
  }

  public Release getReleaseByName(String releaseName) {
    return releaseRepository.findReleaseByName(releaseName);
  }

  public List<Release> getCompletedReleases() throws IllegalReleaseStateException {
    return releaseRepository.findCompletedReleases();
  }

  /**
   * Optionally returns a {@code ReleaseView} matching the given name, and for which {@code Submission}s are filtered
   * based on the user's privileges.
   */
  public Optional<ReleaseView> getReleaseViewBySubject(String releaseName, Subject subject) {
    val release = releaseRepository.findReleaseByName(releaseName);
    Optional<ReleaseView> releaseView = Optional.absent();
    if (release != null) {
      // populate project name for submissions
      val projects = getProjects(release, subject);
      val liteProjects = getLiteProjects(projects);
      val submissionFilesMap = getSubmissionFilesByProjectKey(releaseName, release);

      releaseView = Optional.of(new ReleaseView(release, liteProjects, submissionFilesMap));
    }

    return releaseView;
  }

  public List<String> getSignedOffReleases() {
    return getProjectKeysBySubmissionState(SubmissionState.SIGNED_OFF);
  }

  public Release getCompletedRelease(String releaseName) throws IllegalReleaseStateException {
    val release = releaseRepository.findCompletedRelease(releaseName);
    if (release == null) {
      throw new IllegalArgumentException("Release " + releaseName + " is not complete");
    }

    return release;
  }

  /**
   * Returns the {@code NextRelease} (guaranteed not to be null if returned).
   */
  @Synchronized
  public Release getNextRelease() throws IllegalReleaseStateException {
    val nextRelease = releaseRepository.findNextRelease();
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
    return dictionaryRepository.findDictionaryByVersion(version);
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
    } else if (dictionaryRepository.findDictionaryByVersion(dictionaryVersion) == null) {
      throw new ReleaseException("Specified dictionary version not found in DB: " + dictionaryVersion);
    }

    // Just use name and dictionaryVersion from incoming json
    val nextRelease = new Release(initRelease.getName());
    nextRelease.setDictionaryVersion(dictionaryVersion);
    log.info("Saving new release: '{}'", nextRelease.getName());
    releaseRepository.saveNewRelease(nextRelease);

    // after initial release, create initial file system
    Set<String> projects = Sets.newHashSet();
    dccFileSystem.createInitialReleaseFilesystem(nextRelease, projects);
  }

  @Synchronized
  public Release performRelease(@NonNull String nextReleaseName) throws InvalidStateException {
    // Check for next release name
    if (validateEntityName(nextReleaseName) == false) {
      throw new InvalidNameException(nextReleaseName);
    }

    val oldRelease = getNextRelease();
    Release newRelease = null;
    try {
      if (oldRelease == null) {
        val errorMessage = "No current release";
        log.error(errorMessage);
        throw new ReleaseException(errorMessage);
      }
      if (oldRelease.getState() != OPENED) {
        val errorMessage = "Release is not open";
        log.error(errorMessage);
        throw new InvalidStateException(INVALID_STATE, errorMessage);
      }
      if (!oldRelease.isSignOffAllowed()) {
        val errorMessage = "No signed off project in " + oldRelease;
        log.error(errorMessage);
        throw new InvalidStateException(SIGNED_OFF_SUBMISSION_REQUIRED, errorMessage);
      }
      if (oldRelease.isQueued()) {
        val errorMessage = "Some projects are still enqueued in " + oldRelease;
        log.error(errorMessage);
        throw new InvalidStateException(QUEUE_NOT_EMPTY, errorMessage);
      }

      val dictionaryVersion = oldRelease.getDictionaryVersion();
      if (dictionaryVersion == null) {
        val errorMessage = "Could not find a dictionary matching null";
        log.error(errorMessage);
        throw new InvalidStateException(RELEASE_MISSING_DICTIONARY, errorMessage);
      }
      if (releaseRepository.findReleaseByName(nextReleaseName) != null) {
        val errorMessage = "Found a conflicting release for name " + nextReleaseName;
        log.error(errorMessage);
        throw new InvalidStateException(DUPLICATE_RELEASE_NAME, errorMessage);
      }

      // Actually perform the release
      newRelease = performRelease(oldRelease, nextReleaseName, dictionaryVersion);
    } catch (RuntimeException e) {
      val errorMessage = "Unknown exception trying to release " + nextReleaseName;
      log.error(errorMessage);
      throw new ReleaseException(errorMessage, e);
    }

    return newRelease;
  }

  @Synchronized
  public void signOffRelease(Release nextRelease, List<String> projectKeys, String user)
      throws InvalidStateException, DccModelOptimisticLockException {

    String nextReleaseName = nextRelease.getName();
    log.info("signing off {} for {}", projectKeys, nextReleaseName);

    // update release object
    val expectedState = VALID;
    nextRelease.removeFromQueue(projectKeys);
    for (val projectKey : projectKeys) {
      Submission submission = fetchAndCheckSubmission(nextRelease, projectKey, expectedState);

      submissionService.signOff(submission);
    }

    updateReleaseSafely(nextReleaseName, nextRelease);

    // TODO: synchronization (DCC-685), may require cleaning up the FS abstraction (do we really need the project object
    // or is the projectKey sufficient?)

    // Remove validation files in the ".validation" folder (leave normalization files untouched)
    val releaseFs = dccFileSystem.getReleaseFilesystem(nextRelease);
    val projects = projectRepository.findProjects(projectKeys);
    for (val project : projects) {
      releaseFs
          .getSubmissionDirectory(project.getKey())
          .removeValidationFiles();
    }

    // after sign off, send a email to DCC support
    mailService.sendSignoff(user, projectKeys, nextReleaseName);

    log.info("signed off {} for {}", projectKeys, nextReleaseName);
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
  public Release updateRelease(@NonNull String newReleaseName, String newDictionaryVersion) {
    val release = getNextRelease();
    val oldReleaseName = release.getName();
    val oldDictionaryVersion = release.getDictionaryVersion();
    checkState(release.getState() == ReleaseState.OPENED);

    val sameName = oldReleaseName.equals(newReleaseName);
    val sameDictionary = oldDictionaryVersion.equals(newDictionaryVersion);

    if (!validateEntityName(newReleaseName)) {
      throw new ReleaseException("Updated release name " + newReleaseName + " is not valid");
    }
    if (sameName == false && releaseRepository.findReleaseByName(newReleaseName) != null) {
      throw new ReleaseException("New release name " + newReleaseName + " conflicts with an existing release");
    }
    if (newDictionaryVersion == null) {
      throw new ReleaseException("Release must have associated dictionary before being updated");
    }
    if (sameDictionary == false && dictionaryRepository.findDictionaryByVersion(newDictionaryVersion) == null) {
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

    val success = releaseRepository.updateRelease(oldReleaseName, release, newReleaseName, newDictionaryVersion);
    if (success) { // Ensure update was successful
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
  public List<String> getQueuedProjectKeys() {
    return getNextRelease().getQueuedProjectKeys();
  }

  /**
   * Creates a new {@code Submission} and adds it to the current open {@code Release}
   * 
   * @return Current Open Release
   */
  public Release addSubmission(String projectKey, String projectName) {
    log.info("Creating Submission for Project '{}' in current open Release", projectKey);

    val openRelease = releaseRepository.findOpenRelease();
    val submission = new Submission(projectKey, projectName, openRelease.getName());
    log.info("Created Submission '{}'", submission);

    val release = releaseRepository.addReleaseSubmission(openRelease.getName(), submission);

    return release;
  }

  public Submission getSubmission(String projectKey) {
    val release = getNextRelease();

    return getSubmission(release, projectKey);
  }

  public Submission getSubmission(String releaseName, String projectKey) {
    val release = releaseRepository.findReleaseByName(releaseName);
    checkArgument(release != null);

    return getSubmission(release, projectKey);
  }

  public DetailedSubmission getDetailedSubmission(String releaseName, String projectKey) {
    val submission = getSubmission(releaseName, projectKey);
    val liteProject = new LiteProject(checkNotNull(projectRepository.findProject(projectKey)));

    val detailedSubmission = new DetailedSubmission(submission, liteProject);
    detailedSubmission.setSubmissionFiles(getSubmissionFiles(releaseName, projectKey));

    return detailedSubmission;
  }

  @Synchronized
  public List<SubmissionFile> getSubmissionFiles(@NonNull String releaseName, @NonNull String projectKey) {
    val release = releaseRepository.findReleaseByName(releaseName);
    if (release == null) {
      throw new ReleaseException("No such release");
    }

    val dictionary = dictionaryRepository.findDictionaryByVersion(release.getDictionaryVersion());
    if (dictionary == null) {
      throw new ReleaseException("No Dictionary " + release.getDictionaryVersion());
    }

    val submissionFiles = new ArrayList<SubmissionFile>();
    val buildProjectStringPath = new Path(dccFileSystem.buildProjectStringPath(release.getName(), projectKey));

    for (val path : lsFile(dccFileSystem.getFileSystem(), buildProjectStringPath)) {
      submissionFiles.add(getSubmissionFile(dictionary, path));
    }

    return submissionFiles;
  }

  @Synchronized
  public void queueSubmissions(@NonNull Release nextRelease, @NonNull List<QueuedProject> queuedProjects)
      throws InvalidStateException,
      DccModelOptimisticLockException {
    val nextReleaseName = nextRelease.getName();
    log.info("enqueuing {} for {}", queuedProjects, nextReleaseName);

    // Update release object
    nextRelease.enqueue(queuedProjects);

    for (val queuedProject : queuedProjects) {
      val projectKey = queuedProject.getKey();
      val submission = fetchAndCheckSubmission(nextRelease, projectKey, NOT_VALIDATED, VALID, INVALID, ERROR);

      submissionService.queue(submission, queuedProject.getDataTypes());
    }

    updateReleaseSafely(nextReleaseName, nextRelease);
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
  public void dequeueSubmission(@NonNull final QueuedProject nextProject) {
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

        // Actually dequeue the project
        val dequeuedProject = nextRelease.dequeueProject();
        val dequeuedProjectKey = dequeuedProject.getKey();
        if (dequeuedProjectKey.equals(nextProjectKey) == false) { // not recoverable: TODO: create dedicated exception?
          throw new ReleaseException("Mismatch: " + dequeuedProjectKey + " != " + nextProjectKey);
        }

        // Update release object
        val submission = getSubmissionByProjectKey(nextRelease, nextProjectKey); // can't be null
        val currentState = submission.getState();
        val nextState = SubmissionState.VALIDATING;
        if (expectedState != currentState) {
          throw new ReleaseException( // not recoverable
              "Project " + nextProjectKey + " is not " + expectedState + " (" + currentState
                  + " instead), cannot set to " + nextState);
        }

        submissionService.validate(submission, nextProject.getDataTypes());

        // Update corresponding database entity
        updateReleaseSafely(nextReleaseName, nextRelease);

        resetValidationFolder(nextProject.getKey(), nextRelease);

        mailService.sendValidationStarted(nextReleaseName, nextProject.getKey(), nextProject.getEmails());

        log.info("Dequeued {} to validating state for {}", nextProjectKey, nextReleaseName);
        return Optional.absent();
      }

    });
  }

  @Synchronized
  public void removeQueuedSubmissions() {
    log.info("Emptying queue");

    val newState = NOT_VALIDATED;
    val release = getNextRelease();
    val projectKeys = release.getQueuedProjectKeys(); // TODO: what if nextrelease changes in the meantime?

    // FIXME: DCC-901
    updateSubmisions(projectKeys, newState);
    release.emptyQueue();

    updateSubmissions(release.getName(), release.getQueue(), projectKeys, newState); // FIXME: DCC-901
    for (val projectKey : projectKeys) {
      // See spec at https://wiki.oicr.on.ca/display/DCCSOFT/Concurrency#Concurrency-Submissionstatesresetting
      resetValidationFolder(projectKey, release);
    }
  }

  @Synchronized
  public void removeQueuedSubmissions(@NonNull String projectKey) throws InvalidStateException {
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
      updateSubmissions(release.getName(), release.getQueue(), projectKeys, newState);

      log.info("Resetting file system '{}' project validation folder", projectKey);
      resetValidationFolder(projectKey, release);
    }
  }

  @Synchronized
  public void resetSubmissions(@NonNull String releaseName, @NonNull Iterable<String> projectKeys) {
    for (val projectKey : projectKeys) {
      resetSubmission(releaseName, projectKey, Optional.<Path> absent());
    }
  }

  @Synchronized
  public void resetSubmission(@NonNull String releaseName, @NonNull String projectKey, @NonNull Optional<Path> path) {
    val release = releaseRepository.findReleaseByName(releaseName);
    val dictionary = dictionaryRepository.findDictionaryByVersion(release.getDictionaryVersion());
    val submission = release.getSubmissionByProjectKey(projectKey).get();

    // Update in-memory state
    submissionService.modify(release, submission, dictionary, path);

    // Update persisted state
    updateSubmission(releaseName, submission);

    // Update file system state
    resetValidationFolder(projectKey, release);
  }

  /**
   * Attempts to resolve the given project, if the project is found the given state is set for it.<br>
   * <p>
   * This method is robust enough to handle rare cases like when:<br>
   * - the queue was emptied by an admin in another thread (TODO: complete, this is only partially supported now)<br>
   * - the optimistic lock on Release cannot be obtained (retries a number of time before giving up)<br>
   */
  @Synchronized
  public void resolveSubmission(@NonNull QueuedProject project, @NonNull ValidationOutcome outcome,
      @NonNull SubmissionReport submissionReport) {
    // Update the in-memory submission state
    val projectKey = project.getKey();
    val emails = project.getEmails();
    val dataTypes = project.getDataTypes();
    val release = getNextRelease();
    val submission = getSubmission(release, projectKey);

    submissionService.resolve(submission, dataTypes, outcome, submissionReport, getNextDictionary());

    log.info("Resolving project '{}' to submission state '{}'", projectKey, submission.getState());
    updateSubmission(release.getName(), submission);

    if (!emails.isEmpty()) {
      log.info("Sending notification emails for project '{}'...", projectKey);
      mailService.sendValidationResult(release.getName(), projectKey, emails, submission.getState());
    }

    log.info("Resolved project '{}'", projectKey);
  }

  private Release performRelease(@NonNull Release oldRelease, @NonNull String nextReleaseName,
      @NonNull String dictionaryVersion) {
    // Create new release entity
    val newRelease = createNextRelease(oldRelease, nextReleaseName, dictionaryVersion);

    // Set up new release file system counterpart
    setUpNewReleaseFileSystem(oldRelease, newRelease);

    // Must happen AFTER creating the new release object and setting up the file system (both operations need the old
    // release in its pre-completion state)
    log.info("Completing old release entity object: '{}'", oldRelease.getName());
    oldRelease.complete();

    // Persist modified entity objects
    log.info("Closing dictionary: '{}'", dictionaryVersion);
    dictionaryRepository.closeDictionary(dictionaryVersion);

    log.info("Updating completed release: '{}'", oldRelease.getName());
    releaseRepository.updateCompletedRelease(oldRelease);

    log.info("Saving new release: '{}'", newRelease.getName());
    releaseRepository.saveNewRelease(newRelease);

    return newRelease;
  }

  private Release createNextRelease(@NonNull Release oldRelease, @NonNull String name, @NonNull String dictionaryVersion) {
    val nextRelease = new Release(name);
    nextRelease.setDictionaryVersion(dictionaryVersion);
    nextRelease.setState(ReleaseState.OPENED);

    for (val submission : oldRelease.getSubmissions()) {
      val newSubmission = submissionService.release(nextRelease, submission);

      nextRelease.addSubmission(newSubmission);
    }

    return nextRelease;
  }

  private void setUpNewReleaseFileSystem(@NonNull Release oldRelease, @NonNull Release nextRelease) {
    dccFileSystem.getReleaseFilesystem(nextRelease)
        .setUpNewReleaseFileSystem(
            oldRelease.getName(), // Remove after DCC-1940
            nextRelease.getName(),

            // The release file system
            dccFileSystem.getReleaseFilesystem(oldRelease),

            // The signed off projects
            getProjectKeys(filter(oldRelease.getSubmissions(), SIGNED_OFF_PROJECTS_PREDICATE)),

            // The remaining projects
            getProjectKeys(filter(oldRelease.getSubmissions(), not(SIGNED_OFF_PROJECTS_PREDICATE))));
  }

  /**
   * Empties .validation dir to ensure the cascade runs
   * 
   * TODO: only taken out of resetSubmission() until DCC-901 is done (to allow code that calls deprecated methods
   * instead of resetSubmission() to still be able to empty those directories)
   */
  private void resetValidationFolder(@NonNull String projectKey, @NonNull Release release) {
    log.info("Resetting validation folder for '{}' in release '{}'", projectKey, release.getName());
    dccFileSystem.getReleaseFilesystem(release).resetValidationFolder(projectKey);
  }

  /**
   * Updates the release with the given name, there must be a matching release.<br>
   * 
   * Concurrency is handled with <code>{@link BaseEntity#internalVersion}</code> (optimistic lock).
   * 
   * @throws DccModelOptimisticLockException if optimistic lock fails
   * @throws ReleaseException if the update fails for other reasons (probably not recoverable)
   */
  private void updateReleaseSafely(@NonNull String originalReleaseName, @NonNull Release updatedRelease)
      throws DccModelOptimisticLockException {
    UpdateResults<Release> update = null;
    try {
      update = releaseRepository.updateRelease(originalReleaseName, updatedRelease);
    } catch (ConcurrentModificationException e) { // see method comments for why this could be thrown
      log.warn("a possibly recoverable concurrency issue arose when trying to update release {}", originalReleaseName);
      throw new DccModelOptimisticLockException(e);
    }
    if (update == null || update.getHadError()) {
      log.error("an unrecoverable error happenend when trying to update release {}", originalReleaseName);
      throw new ReleaseException(String.format("failed to update release %s", originalReleaseName));
    }
  }

  @Deprecated
  private void updateSubmisions(@NonNull List<String> projectKeys, @NonNull SubmissionState state) {
    val releaseName = getNextRelease().getName();
    for (val projectKey : projectKeys) {
      val submission = getSubmission(releaseName, projectKey);

      submission.setState(state);
    }
  }

  @Deprecated
  private void updateSubmissions(@NonNull String releaseName, @NonNull List<QueuedProject> queue,
      @NonNull List<String> projectKeys,
      @NonNull SubmissionState newState) {
    releaseRepository.updateReleaseQueue(releaseName, queue);

    for (val projectKey : projectKeys) {
      releaseRepository.updateReleaseSubmissionState(releaseName, projectKey, newState);
    }
  }

  private void updateSubmission(@NonNull String releaseName, @NonNull Submission submission) {
    releaseRepository.updateReleaseSubmission(releaseName, submission);
  }

  private List<Project> getProjects(Release release, Subject user) {
    val builder = ImmutableList.<String> builder();
    for (val projectKey : release.getProjectKeys()) {
      val privilege = projectViewPrivilege(projectKey);
      val viewable = user.isPermitted(privilege);
      if (viewable) {
        builder.add(projectKey);
      }
    }

    return projectRepository.findProjects(builder.build());
  }

  private Submission getSubmission(Release release, String projectKey) {
    val optional = release.getSubmissionByProjectKey(projectKey);
    if (optional.isPresent()) {
      return optional.get();
    }

    throw new ReleaseException(format("There is no project \"%s\" associated with release \"%s\"",
        projectKey, release.getName()));
  }

  private SubmissionFile getSubmissionFile(Dictionary dictionary, Path path) {
    val fileName = path.getName();
    val fileStatus = HadoopUtils.getFileStatus(dccFileSystem.getFileSystem(), path);
    val lastUpdate = new Date(fileStatus.getModificationTime());
    val size = fileStatus.getLen();

    val fileSchema = dictionary.getFileSchemaByFileName(fileName);
    String schemaName = null;
    String dataType = null;
    if (fileSchema.isPresent()) {
      schemaName = fileSchema.get().getName();
      dataType = fileSchema.get().getDataType().name();
    } else {
      schemaName = null;
      dataType = null;
    }

    return new SubmissionFile(fileName, lastUpdate, size, schemaName, dataType);
  }

  private Submission fetchAndCheckSubmission(@NonNull Release nextRelease, @NonNull String projectKey,
      SubmissionState... expectedStates)
      throws InvalidStateException {
    val optional = nextRelease.getSubmissionByProjectKey(projectKey);
    if (!optional.isPresent()) {
      val errorMessage = "project " + projectKey + " cannot be found";
      log.error(errorMessage);
      throw new InvalidStateException(ServerErrorCode.PROJECT_KEY_NOT_FOUND, errorMessage);
    }

    val submission = optional.get();
    val currentState = submission.getState();
    val invalid = !contains(expectedStates, currentState);
    if (invalid) {
      val errorMessage = format("project %s is not in expected states %s (%s instead",
          projectKey, Arrays.toString(expectedStates), currentState);
      log.error(errorMessage);
      throw new InvalidStateException(ServerErrorCode.INVALID_STATE, errorMessage, currentState);
    }

    return submission;
  }

  private List<String> getProjectKeysBySubmissionState(@NonNull final SubmissionState state) {
    val builder = ImmutableList.<String> builder();
    val submissions = getNextRelease().getSubmissions();
    for (val submission : submissions) {
      if (state.equals(submission.getState())) {
        builder.add(submission.getProjectKey());
      }
    }

    return builder.build();
  }

  /**
   * Attempts to retrieve a submission for the given project key provided from the release object provided (no database
   * call).
   * <p>
   * Throws a {@code ReleaseException} if not matching submission is found.
   */
  private Submission getSubmissionByProjectKey(@NonNull Release release, @NonNull String projectKey) {
    val optional = release.getSubmissionByProjectKey(projectKey);
    if (!optional.isPresent()) {
      throw new ReleaseException(format("There is no project '%s' associated with release '%s'",
          projectKey, release.getName()));
    }

    return optional.get();
  }

  private List<LiteProject> getLiteProjects(List<Project> projects) {
    val builder = ImmutableList.<LiteProject> builder();
    for (val project : projects) {
      val liteProject = new LiteProject(project);

      builder.add(liteProject);
    }

    return builder.build();
  }

  private Map<String, List<SubmissionFile>> getSubmissionFilesByProjectKey(String releaseName, Release release) {
    val builder = ImmutableMap.<String, List<SubmissionFile>> builder();
    for (val projectKey : release.getProjectKeys()) {
      val submissionFiles = getSubmissionFiles(releaseName, projectKey);

      builder.put(projectKey, submissionFiles);
    }

    return builder.build();
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

}
