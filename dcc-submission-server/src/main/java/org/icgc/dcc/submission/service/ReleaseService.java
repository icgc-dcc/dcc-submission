/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.lsFile;
import static org.icgc.dcc.submission.core.auth.Authorizations.getUsername;
import static org.icgc.dcc.submission.core.auth.Authorizations.hasSpecificProjectPrivilege;
import static org.icgc.dcc.submission.core.util.NameValidator.validateEntityName;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.release.model.SubmissionState.NOT_VALIDATED;
import static org.icgc.dcc.submission.release.model.SubmissionState.SIGNED_OFF;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.DUPLICATE_RELEASE_NAME;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.INVALID_STATE;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.QUEUE_NOT_EMPTY;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.RELEASE_MISSING_DICTIONARY;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.SIGNED_OFF_SUBMISSION_REQUIRED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.common.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.core.InvalidStateException;
import org.icgc.dcc.submission.core.model.DccModelOptimisticLockException;
import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.core.util.NameValidator;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionFile;
import org.icgc.dcc.submission.fs.SubmissionFileEvent;
import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.ReleaseView;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.repository.DictionaryRepository;
import org.icgc.dcc.submission.repository.ProjectRepository;
import org.icgc.dcc.submission.repository.ReleaseRepository;
import org.icgc.dcc.submission.web.InvalidNameException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import lombok.NonNull;
import lombok.Synchronized;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReleaseService extends AbstractService {

  /**
   * Dependencies.
   */
  private final DccFileSystem dccFileSystem;
  private final ReleaseRepository releaseRepository;
  private final DictionaryRepository dictionaryRepository;
  private final ProjectRepository projectRepository;

  @Autowired
  public ReleaseService(
      @NonNull final MailService mailService,
      @NonNull final DccFileSystem dccFileSystem,
      @NonNull final ReleaseRepository releaseRepository,
      @NonNull final DictionaryRepository dictionaryRepository,
      @NonNull final ProjectRepository projectRepository) {
    super(mailService);
    this.dccFileSystem = dccFileSystem;
    this.releaseRepository = releaseRepository;
    this.dictionaryRepository = dictionaryRepository;
    this.projectRepository = projectRepository;
  }

  /**
   * Returns the number of releases.
   */
  public long countReleases() {
    return releaseRepository.countReleases();
  }

  /**
   * Returns the number of releases that are in the {@link ReleaseState#OPENED} state. It is expected that there always
   * ever be one at a time.
   */
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
  public List<Release> getReleasesBySubject(Authentication authentication) {
    log.debug("getting releases for {}", getUsername(authentication));

    List<Release> releases = releaseRepository.findReleaseSummaries();
    log.debug("Number of releases:{} ", releases.size());

    // Filter out all the submissions that the current user can not see
    for (val release : releases) {
      val builder = ImmutableList.<Submission> builder();
      for (val submission : release.getSubmissions()) {
        val permitted = hasSpecificProjectPrivilege(authentication, submission.getProjectKey());
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
  public Optional<ReleaseView> getReleaseViewBySubject(String releaseName, Authentication authentication) {
    val release = releaseRepository.findReleaseSummaryByName(releaseName);
    Optional<ReleaseView> releaseView = Optional.absent();
    if (release != null) {
      // populate project name for submissions
      val projects = getProjects(release, authentication);
      val submissionFilesMap = getSubmissionFilesByProjectKey(releaseName, release);

      releaseView = Optional.of(new ReleaseView(release, projects, submissionFilesMap));
    }

    return releaseView;
  }

  public List<String> getSignedOffReleases() {
    val submissions = getNextRelease().getSubmissions();
    return getProjectKeysBySubmissionState(submissions, SIGNED_OFF);
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
  public Release getNextRelease() {
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
    val releaseName = initRelease.getName();
    if (!NameValidator.validateEntityName(releaseName)) {
      throw new InvalidNameException(initRelease.getName());
    }

    val dictionaryVersion = initRelease.getDictionaryVersion();
    log.info("Dictionary version used: '{}'", dictionaryVersion);

    val missing = dictionaryVersion == null;
    if (missing) {
      throw new ReleaseException("Dictionary version must not be null for initial release '%s'", releaseName);
    } else if (dictionaryRepository.findDictionaryByVersion(dictionaryVersion) == null) {
      throw new ReleaseException("Specified dictionary version '%s' not found", dictionaryVersion);
    }

    // Just use name and dictionaryVersion from incoming json
    val nextRelease = new Release(releaseName);
    nextRelease.setDictionaryVersion(dictionaryVersion);
    log.info("Saving new release: '{}'", releaseName);
    releaseRepository.saveNewRelease(nextRelease);

    // After initial release, create initial file system
    val projects = Sets.<String> newHashSet();
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
  public void signOffRelease(Iterable<String> projectKeys, String user) throws InvalidStateException,
      DccModelOptimisticLockException {
    val release = getNextRelease();
    String releaseName = release.getName();
    log.info("signing off {} for {}", projectKeys, releaseName);

    release.removeFromQueue(projectKeys);
    val filePatternToTypeMap = dictionaryRepository.getFilePatternToTypeMap(release.getDictionaryVersion());
    for (val projectKey : projectKeys) {
      val submissionFiles = getSubmissionFiles(release.getName(), projectKey, filePatternToTypeMap);
      val submission = release.getSubmission(projectKey).get();

      //
      // Transition
      //

      submission.signOff(submissionFiles);
    }

    releaseRepository.updateRelease(releaseName, release);

    // Remove validation files in the ".validation" folder (leave normalization files untouched)
    val releaseFs = dccFileSystem.getReleaseFilesystem(release);
    val projects = projectRepository.findProjects(projectKeys);
    for (val project : projects) {
      releaseFs.getSubmissionDirectory(project.getKey()).removeValidationFiles();
    }

    // after sign off, send a email to DCC support
    mailService.sendSignoff(user, projectKeys, releaseName);

    log.info("signed off {} for {}", projectKeys, releaseName);
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
      throw new ReleaseException("Updated release name '%s' is not valid", newReleaseName);
    }
    if (sameName == false && releaseRepository.findReleaseByName(newReleaseName) != null) {
      throw new ReleaseException("New release name '%s' conflicts with an existing release", newReleaseName);
    }
    if (newDictionaryVersion == null) {
      throw new ReleaseException("Release must have associated dictionary before being updated");
    }
    if (sameDictionary == false && dictionaryRepository.findDictionaryByVersion(newDictionaryVersion) == null) {
      throw new ReleaseException("Release must point to an existing dictionary, no match for '%s'",
          newDictionaryVersion);
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
      resetSubmissions();
    }

    return release;
  }

  @Synchronized
  public List<String> getQueuedProjectKeys() {
    return releaseRepository.findNextReleaseQueue().getQueuedProjectKeys();
  }

  /**
   * Creates a new {@code Submission} and adds it to the current open {@code Release}
   * 
   * @return Current Open Release
   */
  @Synchronized
  public void addSubmission(String projectKey, String projectName) {
    log.info("Creating Submission for Project '{}' in current open Release", projectKey);
    val release = releaseRepository.findOpenRelease();
    val submissionPath = dccFileSystem.createNewProjectDirectoryStructure(release.getName(), projectKey);
    val submissionFiles = getSubmissionFiles(release.getName(), release.getDictionaryVersion(), projectKey);
    val submission = new Submission(projectKey, projectName, release.getName(), NOT_VALIDATED);

    //
    // Transition
    //

    submission.initialize(submissionFiles);
    releaseRepository.addReleaseSubmission(release.getName(), submission);

    log.info("Created Submission '{}' with directory '{}'", submission, submissionPath);
  }

  public Submission getSubmission(String projectKey) {
    val release = getNextRelease();

    return getSubmission(release, projectKey);
  }

  public boolean submissionExists(String releaseName, String projectKey) {
    return getSubmission(releaseName, projectKey) != null;
  }

  public Submission getSubmission(String releaseName, String projectKey) {
    val release = releaseRepository.findReleaseByName(releaseName);
    val missing = release == null;
    if (missing) {
      throw new ReleaseException(
          "No release with name '%s' found when attempting to get submission with project key '%s'",
          releaseName, projectKey);
    }

    return getSubmission(release, projectKey);
  }

  public DetailedSubmission getDetailedSubmission(String releaseName, String projectKey) {
    val submissionFiles = getSubmissionFiles(releaseName, projectKey);
    val project = projectRepository.findProject(projectKey);
    val submission = getSubmission(releaseName, projectKey);
    val detailedSubmission = new DetailedSubmission(submission, project);
    detailedSubmission.setSubmissionFiles(submissionFiles);

    return detailedSubmission;
  }

  public List<SubmissionFile> getSubmissionFiles(@NonNull String releaseName, @NonNull String projectKey) {
    val release =
        checkNotNull(releaseRepository.findReleaseByName(releaseName), "No release with name '%s'", releaseName);
    return getSubmissionFiles(release.getName(), release.getDictionaryVersion(), projectKey);
  }

  private List<SubmissionFile> getSubmissionFiles(
      @NonNull String releaseName, @NonNull String dictionaryVersion, @NonNull String projectKey) {
    return getSubmissionFiles(releaseName, projectKey, dictionaryRepository.getFilePatternToTypeMap(dictionaryVersion));
  }

  private List<SubmissionFile> getSubmissionFiles(
      @NonNull String releaseName, @NonNull String projectKey, @NonNull Map<String, FileType> filePatternToTypeMap) {
    val submissionFiles = new ArrayList<SubmissionFile>();
    val projectStringPath = new Path(dccFileSystem.buildProjectStringPath(releaseName, projectKey));

    for (val path : lsFile(dccFileSystem.getFileSystem(), projectStringPath)) {
      try {
        submissionFiles.add(getSubmissionFile(filePatternToTypeMap, path));
      } catch (Exception e) {
        // This could happen if the file was renamed or removed in the meantime
        log.warn("Could not get submission file '{}': {}", path, e.getMessage());
      }
    }

    return submissionFiles;
  }

  public Optional<FileReport> getFileReport(String releaseName, String projectKey, String fileName) {
    Optional<FileReport> optional = Optional.absent();
    val submission = getSubmission(releaseName, projectKey);
    if (submission != null) {
      val report = submission.getReport();
      if (report != null) {
        optional = MongoMaxSizeHack.augmentScriptErrors(
            report.getFileReport(fileName),
            releaseRepository, dictionaryRepository);
      }
    }

    return optional;
  }

  @Synchronized
  public void queueSubmissions(@NonNull List<QueuedProject> queuedProjects) throws InvalidStateException,
      DccModelOptimisticLockException {
    val release = getNextRelease();
    val releaseName = release.getName();
    log.info("Enqueuing {} for {}", queuedProjects, releaseName);

    // Update release object
    release.enqueue(queuedProjects);

    val filePatternToTypeMap = dictionaryRepository.getFilePatternToTypeMap(release.getDictionaryVersion());
    for (val queuedProject : queuedProjects) {
      val projectKey = queuedProject.getKey();
      val submission = release.getSubmission(projectKey).get();
      val submissionFiles = getSubmissionFiles(release.getName(), projectKey, filePatternToTypeMap);

      //
      // Transition
      //

      submission.queueRequest(submissionFiles, queuedProject.getDataTypes());
    }

    releaseRepository.updateRelease(releaseName, release);
    log.info("Enqueued {} for {}", queuedProjects, releaseName);
  }

  /**
   * Attempts to set the given project to VALIDATING.<br>
   * <p>
   * This method is robust enough to handle rare cases like when:<br>
   * - the queue was emptied by an admin in another thread (TODO: complete, this is only partially supported now)<br>
   * - the optimistic lock on Release cannot be obtained (retries a number of time before giving up)<br>
   * @param nextReport
   * @param dataTypes
   */
  @Synchronized
  public void dequeueSubmission(@NonNull final QueuedProject queuedProject, @NonNull final Report nextReport) {
    val projectKey = queuedProject.getKey();

    val description = format("validate project '%s'", projectKey);
    log.info("Attempting to {}", description);

    withRetry(description, new Callable<Optional<?>>() {

      @Override
      public Optional<?> call() throws DccModelOptimisticLockException {
        val release = getNextRelease();
        val releaseName = release.getName();
        log.info("Dequeuing {} to validating for {}", projectKey, releaseName);

        // In-memory - queue transition
        val dequeuedProject = release.dequeueProject();
        val dequeuedProjectKey = dequeuedProject.getKey();
        if (dequeuedProjectKey.equals(projectKey) == false) {
          log.error("Mismatch: '{}' != '{}'", dequeuedProjectKey, projectKey);
          throw new ReleaseException("Mismatch: '%s' != '%s'", dequeuedProjectKey, projectKey);
        }

        // In-memory - submission resolve
        val submissionFiles =
            getSubmissionFiles(release.getName(), release.getDictionaryVersion(), queuedProject.getKey());
        val submission = release.getSubmission(projectKey).get();

        // In-memory - submission transition
        submission.startValidation(submissionFiles, queuedProject.getDataTypes(), nextReport);

        // Mongo - queue / submission persist
        log.info("--> Updating db release / submission state for '{}'...", projectKey);
        releaseRepository.updateRelease(releaseName, release);
        log.info("<-- Finished updating db release / submission state for '{}'", projectKey);

        // HDFS - validation files removal
        resetValidationFolder(queuedProject.getKey(), release);

        // Mail - send
        mailService.sendValidationStarted(releaseName, queuedProject.getKey(), queuedProject.getEmails());

        log.info("Dequeued {} to validating state for {}", projectKey, releaseName);
        return Optional.absent();
      }

    });
  }

  @Synchronized
  public void removeQueuedSubmissions(@NonNull String... targets) throws InvalidStateException {
    val release = getNextRelease();
    val releaseName = release.getName();
    val projectKeys = targets.length > 0 ? release.getQueuedProjectKeys() : ImmutableList.<String> copyOf(targets);

    log.info("Deleting queued request for project(s) '{}'", projectKeys);
    val queue = ImmutableList.<QueuedProject> copyOf(release.getQueue());
    val filePatternToTypeMap = dictionaryRepository.getFilePatternToTypeMap(release.getDictionaryVersion());
    for (val queuedProject : queue) {
      val projectKey = queuedProject.getKey();
      val dataTypes = queuedProject.getDataTypes();

      val remove = projectKeys.contains(projectKey);
      if (remove) {
        val submission = release.getSubmission(projectKey).get();
        val submissionFiles = getSubmissionFiles(releaseName, projectKey, filePatternToTypeMap);

        //
        // Transition
        //

        submission.cancelValidation(submissionFiles, dataTypes);
        release.removeFromQueue(projectKey);
        resetValidationFolder(projectKey, release);
      }
    }

    releaseRepository.updateRelease(releaseName, release);
  }

  public void resetSubmissions() {
    resetSubmissions(getNextRelease().getProjectKeys());
  }

  @Synchronized
  public void resetInvalidSubmissions() {
    val release = getNextRelease();
    val filePatternToTypeMap = dictionaryRepository.getFilePatternToTypeMap(release.getDictionaryVersion());
    for (val projectKey : release.getInvalidProjectKeys()) {
      resetSubmission(release, projectKey, filePatternToTypeMap);
    }
  }

  @Synchronized
  public void resetSubmissions(Iterable<String> projects) {
    val release = getNextRelease();
    val filePatternToTypeMap = dictionaryRepository.getFilePatternToTypeMap(release.getDictionaryVersion());

    for (val projectKey : projects) {
      resetSubmission(release, projectKey, filePatternToTypeMap);
    }
  }

  @Synchronized
  public Submission modifySubmission(@NonNull String releaseName, @NonNull String projectKey,
      @NonNull SubmissionFileEvent event) {

    val release = releaseRepository.findReleaseByName(releaseName);
    val submissionFiles = getSubmissionFiles(release.getName(), release.getDictionaryVersion(), projectKey);
    val submission = release.getSubmission(projectKey).get();

    //
    // Transition
    //

    submission.modifyFile(submissionFiles, event);
    releaseRepository.updateReleaseSubmission(releaseName, submission);
    resetValidationFolder(projectKey, release);

    return submission;
  }

  /**
   * Attempts to resolve the given project, if the project is found the given state is set for it.<br>
   * <p>
   * This method is robust enough to handle rare cases like when:<br>
   * - the queue was emptied by an admin in another thread (TODO: complete, this is only partially supported now)<br>
   * - the optimistic lock on Release cannot be obtained (retries a number of time before giving up)<br>
   */
  @Synchronized
  public void resolveSubmission(@NonNull QueuedProject project, @NonNull Outcome outcome, @NonNull Report newReport) {
    // Update the in-memory submission state
    val projectKey = project.getKey();
    val emails = project.getEmails();
    val release = getNextRelease();
    val submissionFiles = getSubmissionFiles(release.getName(), release.getDictionaryVersion(), projectKey);
    val submission = getSubmission(release, projectKey);

    //
    // Transition
    //

    submission.finishValidation(submissionFiles, project.getDataTypes(), outcome, newReport);
    releaseRepository.updateReleaseSubmission(release.getName(), submission);

    if (!emails.isEmpty()) {
      log.info("Sending notification emails for project '{}'...", projectKey);
      mailService.sendValidationResult(release.getName(), projectKey, emails, submission.getState(), newReport);
    }

    log.info("Resolved project '{}'", projectKey);
  }

  private Release performRelease(@NonNull Release oldRelease, @NonNull String nextReleaseName,
      @NonNull String dictionaryVersion) {

    // Create new release entity
    val newRelease = new Release(nextReleaseName, dictionaryVersion);
    val filePatternToTypeMap = dictionaryRepository.getFilePatternToTypeMap(oldRelease.getDictionaryVersion());
    for (val submission : oldRelease.getSubmissions()) {
      val submissionFiles = getSubmissionFiles(oldRelease.getName(), submission.getProjectKey(), filePatternToTypeMap);

      //
      // Transition
      //

      val newSubmission = submission.closeRelease(submissionFiles, newRelease);

      newRelease.addSubmission(newSubmission);
    }

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

  private void setUpNewReleaseFileSystem(@NonNull Release oldRelease, @NonNull Release nextRelease) {
    val oldReleaseFileSystem = dccFileSystem.getReleaseFilesystem(oldRelease);

    // Copy all files from the old to the new release
    dccFileSystem.getReleaseFilesystem(nextRelease)
        .setUpNewReleaseFileSystem(
            nextRelease.getName(),
            oldReleaseFileSystem,
            oldRelease.getProjectKeys());
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

  private Submission resetSubmission(
      @NonNull Release release, @NonNull String projectKey, @NonNull Map<String, FileType> filePatternToTypeMap) {
    val submission = release.getSubmission(projectKey).get();
    val submissionFiles = getSubmissionFiles(release.getName(), projectKey, filePatternToTypeMap);

    //
    // Transition
    //

    submission.reset(submissionFiles);
    releaseRepository.updateReleaseSubmission(release.getName(), submission);
    resetValidationFolder(projectKey, release);

    return submission;
  }

  private List<Project> getProjects(Release release, Authentication authentication) {
    val builder = ImmutableList.<String> builder();
    for (val projectKey : release.getProjectKeys()) {
      val viewable = hasSpecificProjectPrivilege(authentication, projectKey);
      if (viewable) {
        builder.add(projectKey);
      }
    }

    return projectRepository.findProjects(builder.build());
  }

  private Submission getSubmission(Release release, String projectKey) {
    val optional = release.getSubmission(projectKey);
    if (optional.isPresent()) {
      return optional.get();
    }

    throw new ReleaseException("There is no project '%s' associated with release '%s'", projectKey, release.getName());
  }

  private SubmissionFile getSubmissionFile(Map<String, FileType> filePatternToTypeMap, Path filePath)
      throws IOException {
    val fileName = filePath.getName();
    val fileStatus = HadoopUtils.getFileStatus(dccFileSystem.getFileSystem(), filePath).get();
    val fileLastUpdate = new Date(fileStatus.getModificationTime());
    val fileSize = fileStatus.getLen();
    val fileType = getFileType(filePatternToTypeMap, fileName).orNull();

    return new SubmissionFile(fileName, fileLastUpdate, fileSize, fileType);
  }

  private Map<String, List<SubmissionFile>> getSubmissionFilesByProjectKey(String releaseName, Release release) {
    val filePatternToTypeMap = dictionaryRepository.getFilePatternToTypeMap(release.getDictionaryVersion());
    val builder = ImmutableMap.<String, List<SubmissionFile>> builder();
    for (val projectKey : release.getProjectKeys()) {
      val submissionFiles = getSubmissionFiles(releaseName, projectKey, filePatternToTypeMap);

      builder.put(projectKey, submissionFiles);
    }

    return builder.build();
  }

  private static List<String> getProjectKeysBySubmissionState(@NonNull List<Submission> submissions,
      @NonNull final SubmissionState state) {
    val projectKeys = ImmutableList.<String> builder();
    for (val submission : submissions) {
      if (state.equals(submission.getState())) {
        projectKeys.add(submission.getProjectKey());
      }
    }

    return projectKeys.build();
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

  private Optional<FileType> getFileType(Map<String, FileType> filePatternToTypeMap, String fileName) {
    for (val pattern : filePatternToTypeMap.keySet()) {
      if (Pattern.compile(pattern).matcher(fileName).matches()) {
        return Optional.of(filePatternToTypeMap.get(pattern));
      }
    }
    return Optional.<FileType> absent();
  }

}
