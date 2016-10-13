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
package org.icgc.dcc.submission.server.service;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.lsFile;
import static org.icgc.dcc.submission.core.util.NameValidator.validateEntityName;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.NOT_VALIDATED;
import static org.icgc.dcc.submission.release.model.SubmissionState.SIGNED_OFF;
import static org.icgc.dcc.submission.server.web.ServerErrorCode.DUPLICATE_RELEASE_NAME;
import static org.icgc.dcc.submission.server.web.ServerErrorCode.INVALID_STATE;
import static org.icgc.dcc.submission.server.web.ServerErrorCode.QUEUE_NOT_EMPTY;
import static org.icgc.dcc.submission.server.web.ServerErrorCode.RELEASE_MISSING_DICTIONARY;
import static org.icgc.dcc.submission.server.web.ServerErrorCode.SIGNED_OFF_SUBMISSION_REQUIRED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import lombok.NonNull;
import lombok.Synchronized;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.common.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.core.model.DccModelOptimisticLockException;
import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.report.FileReport;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.core.util.NameValidator;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionFile;
import org.icgc.dcc.submission.fs.SubmissionFileEvent;
import org.icgc.dcc.submission.fs.SubmissionFileSystem;
import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.ReleaseView;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.server.core.InvalidStateException;
import org.icgc.dcc.submission.server.repository.DictionaryRepository;
import org.icgc.dcc.submission.server.repository.ProjectRepository;
import org.icgc.dcc.submission.server.repository.ReleaseRepository;
import org.icgc.dcc.submission.server.web.InvalidNameException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

@Slf4j
public class ReleaseService extends AbstractService {

  /**
   * Dependencies.
   */
  private final SubmissionFileSystem submissionFileSystem;
  private final ReleaseRepository releaseRepository;
  private final DictionaryRepository dictionaryRepository;
  private final ProjectRepository projectRepository;
  private final SubmissionService submissionService;

  @Autowired
  public ReleaseService(
      @NonNull final MailService mailService,
      @NonNull final SubmissionFileSystem submissionFileSystem,
      @NonNull final ReleaseRepository releaseRepository,
      @NonNull final DictionaryRepository dictionaryRepository,
      @NonNull final ProjectRepository projectRepository,
      @NonNull final SubmissionService submissionService) {
    super(mailService);
    this.submissionFileSystem = submissionFileSystem;
    this.releaseRepository = releaseRepository;
    this.dictionaryRepository = dictionaryRepository;
    this.projectRepository = projectRepository;
    this.submissionService = submissionService;
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

  public List<Release> getCompletedReleases() throws IllegalReleaseStateException {
    return releaseRepository.findCompletedReleases();
  }

  /**
   * Optionally returns a {@code ReleaseView} matching the given name, and for which {@code Submission}s are filtered
   * based on the user's privileges.
   */
  public Optional<ReleaseView> getReleaseViewBySubject(String releaseName, Authentication authentication) {
    val release = releaseRepository.findReleaseSummaryByName(releaseName);
    val submissions = submissionService.findSubmissionsByProjectKey(releaseName);
    Optional<ReleaseView> releaseView = Optional.absent();
    if (release != null) {
      // populate project name for submissions
      val projectKeys = submissionService.findReleaseProjectKeys(releaseName);
      val projects = projectRepository.findProjects(projectKeys);
      // TODO: Optimize. Looks like the method looks project keys itself.
      val submissionFilesMap = getSubmissionFilesByProjectKey(release);

      releaseView = Optional.of(new ReleaseView(release, submissions, projects, submissionFilesMap));
    }

    return releaseView;
  }

  public List<String> getSignedOffReleases() {
    val nextRelease = getNextRelease();
    val submissions = submissionService.findSubmissions(nextRelease.getName());

    return getProjectKeysBySubmissionState(submissions, SIGNED_OFF);
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
    val submissions = submissionService.findSubmissionsByProjectKey(releaseName);
    submissionFileSystem.createInitialReleaseFilesystem(nextRelease, submissions, projects);
  }

  public boolean isSignOffAllowed(String releaseName) {
    return submissionService.findSubmissionStateByReleaseName(releaseName).stream()
        .map(Submission::getState)
        // At least one submission must be signed off on
        .anyMatch(state -> state == SIGNED_OFF);
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
      if (!isSignOffAllowed(oldRelease.getName())) {
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
  public void signOffRelease(Collection<String> projectKeys, String user) throws InvalidStateException,
      DccModelOptimisticLockException {
    val release = getNextRelease();
    val releaseName = release.getName();
    log.info("signing off {} for {}", projectKeys, releaseName);

    release.removeFromQueue(projectKeys);
    val filePatternToTypeMap = dictionaryRepository.getFilePatternToTypeMap(release.getDictionaryVersion());
    val submissions = submissionService.findProjectKeysToSubmissions(releaseName, projectKeys);
    for (val projectKey : projectKeys) {
      val submissionFiles = getSubmissionFiles(release.getName(), projectKey, filePatternToTypeMap);
      val submission = submissions.get(projectKey);
      checkNotNullSubmission(releaseName, projectKey, submission);

      //
      // Transition
      //

      submission.signOff(submissionFiles);
    }

    releaseRepository.updateRelease(releaseName, release);
    submissionService.updateExistingSubmissions(submissions.values());

    // Remove validation files in the ".validation" folder (leave normalization files untouched)
    val releaseFs = submissionFileSystem.getReleaseFilesystem(release, submissions);
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
    val submissionPath = submissionFileSystem.createNewProjectDirectoryStructure(release.getName(), projectKey);
    val submissionFiles = getSubmissionFiles(release.getName(), release.getDictionaryVersion(), projectKey);
    val submission = new Submission(projectKey, projectName, release.getName(), NOT_VALIDATED);

    //
    // Transition
    //

    submission.initialize(submissionFiles);
    submissionService.addSubmission(submission);

    log.info("Created Submission '{}' with directory '{}'", submission, submissionPath);
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
    val projectStringPath = new Path(submissionFileSystem.buildProjectStringPath(releaseName, projectKey));

    for (val path : lsFile(submissionFileSystem.getFileSystem(), projectStringPath)) {
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
    val projectKeys = queuedProjects.stream()
        .map(QueuedProject::getKey)
        .collect(toImmutableList());
    val submissions = submissionService.findProjectKeysToSubmissions(releaseName, projectKeys);
    for (val queuedProject : queuedProjects) {
      val projectKey = queuedProject.getKey();
      val submission = submissions.get(projectKey);
      checkNotNullSubmission(releaseName, projectKey, submission);
      val submissionFiles = getSubmissionFiles(release.getName(), projectKey, filePatternToTypeMap);

      //
      // Transition
      //

      submission.queueRequest(submissionFiles, queuedProject.getDataTypes());
    }

    releaseRepository.updateRelease(releaseName, release);
    submissionService.updateExistingSubmissions(submissions.values());
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
        val submissionOpt = submissionService.findSubmission(releaseName, projectKey);
        checkSubmissionExistence(projectKey, releaseName, submissionOpt);
        val submission = submissionOpt.get();

        // In-memory - submission transition
        submission.startValidation(submissionFiles, queuedProject.getDataTypes(), nextReport);

        // Mongo - queue / submission persist
        log.info("--> Updating db release / submission state for '{}'...", projectKey);
        releaseRepository.updateRelease(releaseName, release);
        submissionService.updateSubmission(submission);
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
    val queuedProjectKeys = queue.stream()
        .map(QueuedProject::getKey)
        .collect(toImmutableList());
    val submissions = submissionService.findProjectKeysToSubmissions(releaseName, queuedProjectKeys);
    for (val queuedProject : queue) {
      val projectKey = queuedProject.getKey();
      val dataTypes = queuedProject.getDataTypes();

      val remove = projectKeys.contains(projectKey);
      if (remove) {
        val submission = submissions.get(projectKey);
        checkNotNullSubmission(releaseName, projectKey, submission);
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
    submissionService.updateExistingSubmissions(submissions.values());
  }

  public void resetSubmissions() {
    val projectKeys = submissionService.findReleaseProjectKeys(getNextRelease().getName());
    resetSubmissions(projectKeys);
  }

  @Synchronized
  public void resetInvalidSubmissions() {
    val release = getNextRelease();
    val filePatternToTypeMap = dictionaryRepository.getFilePatternToTypeMap(release.getDictionaryVersion());
    val invalidProjectKeys = submissionService.findSubmissions(release.getName()).stream()
        .filter(submission -> submission.getState() == INVALID)
        .map(Submission::getProjectKey)
        .collect(toImmutableList());

    for (val projectKey : invalidProjectKeys) {
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
    val submissionOpt = submissionService.findSubmission(releaseName, projectKey);
    checkSubmissionExistence(projectKey, releaseName, submissionOpt);
    val submission = submissionOpt.get();

    //
    // Transition
    //

    submission.modifyFile(submissionFiles, event);
    submissionService.updateSubmission(submission);
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
    submissionService.updateSubmission(submission);

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
    val oldReleaseName = oldRelease.getName();
    val oldReleaseSubmissions = submissionService.findSubmissions(oldReleaseName);
    val newSubmissions = ImmutableList.<Submission> builder();
    for (val submission : oldReleaseSubmissions) {
      val submissionFiles = getSubmissionFiles(oldReleaseName, submission.getProjectKey(), filePatternToTypeMap);

      //
      // Transition
      //

      val newSubmission = submission.closeRelease(submissionFiles, newRelease);

      newSubmissions.add(newSubmission);
    }

    // Set up new release file system counterpart
    setUpNewReleaseFileSystem(oldRelease, newRelease);

    // Must happen AFTER creating the new release object and setting up the file system (both operations need the old
    // release in its pre-completion state)
    log.info("Completing old release entity object: '{}'", oldReleaseName);
    oldRelease.complete();

    // Persist modified entity objects
    log.info("Closing dictionary: '{}'", dictionaryVersion);
    dictionaryRepository.closeDictionary(dictionaryVersion);

    log.info("Updating completed release: '{}'", oldReleaseName);
    releaseRepository.updateCompletedRelease(oldRelease);
    submissionService.updateExistingSubmissions(oldReleaseSubmissions);
    submissionService.deleteUnsignedSubmissions(oldReleaseName);

    log.info("Saving new release: '{}'", newRelease.getName());
    releaseRepository.saveNewRelease(newRelease);
    submissionService.addSubmissions(newSubmissions.build());

    return newRelease;
  }

  private void setUpNewReleaseFileSystem(@NonNull Release oldRelease, @NonNull Release nextRelease) {
    val submissions = submissionService.findSubmissionsByProjectKey(oldRelease.getName());
    val oldReleaseFileSystem = submissionFileSystem.getReleaseFilesystem(oldRelease, submissions);

    // Copy all files from the old to the new release
    submissionFileSystem.getReleaseFilesystem(nextRelease, submissions)
        .setUpNewReleaseFileSystem(
            nextRelease.getName(),
            oldReleaseFileSystem,
            submissions.keySet());
  }

  /**
   * Empties .validation dir to ensure the cascade runs
   * 
   * TODO: only taken out of resetSubmission() until DCC-901 is done (to allow code that calls deprecated methods
   * instead of resetSubmission() to still be able to empty those directories)
   */
  private void resetValidationFolder(@NonNull String projectKey, @NonNull Release release) {
    log.info("Resetting validation folder for '{}' in release '{}'", projectKey, release.getName());
    getReleaseFileSystem(release).resetValidationFolder(projectKey);
  }

  private ReleaseFileSystem getReleaseFileSystem(Release release) {
    val releaseName = release.getName();
    val submissions = submissionService.findSubmissionsByProjectKey(releaseName);

    return submissionFileSystem.getReleaseFilesystem(release, submissions);
  }

  private Submission resetSubmission(
      @NonNull Release release, @NonNull String projectKey, @NonNull Map<String, FileType> filePatternToTypeMap) {
    val releaseName = release.getName();
    val submissionOpt = submissionService.findSubmission(releaseName, projectKey);
    checkSubmissionExistence(projectKey, releaseName, submissionOpt);
    val submission = submissionOpt.get();
    val submissionFiles = getSubmissionFiles(releaseName, projectKey, filePatternToTypeMap);

    //
    // Transition
    //

    submission.reset(submissionFiles);
    submissionService.updateSubmission(submission);
    resetValidationFolder(projectKey, release);

    return submission;
  }

  private Submission getSubmission(Release release, String projectKey) {
    val releaseName = release.getName();
    val optional = submissionService.findSubmission(releaseName, projectKey);
    if (optional.isPresent()) {
      return optional.get();
    }

    throw new ReleaseException("There is no project '%s' associated with release '%s'", projectKey, release.getName());
  }

  private SubmissionFile getSubmissionFile(Map<String, FileType> filePatternToTypeMap, Path filePath)
      throws IOException {
    val fileName = filePath.getName();
    val fileStatus = HadoopUtils.getFileStatus(submissionFileSystem.getFileSystem(), filePath).get();
    val fileLastUpdate = new Date(fileStatus.getModificationTime());
    val fileSize = fileStatus.getLen();
    val fileType = getFileType(filePatternToTypeMap, fileName).orNull();

    return new SubmissionFile(fileName, fileLastUpdate, fileSize, fileType, false);
  }

  private Map<String, List<SubmissionFile>> getSubmissionFilesByProjectKey(Release release) {
    val releaseName = release.getName();
    val filePatternToTypeMap = dictionaryRepository.getFilePatternToTypeMap(release.getDictionaryVersion());
    val builder = ImmutableMap.<String, List<SubmissionFile>> builder();
    val projectKeys = submissionService.findReleaseProjectKeys(releaseName);

    for (val projectKey : projectKeys) {
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

  private static void checkNotNullSubmission(String releaseName, String projectKey, Submission submission) {
    checkNotNull(submission, "Failed to find submission for release '%s' and project '%'", releaseName, projectKey);
  }

  private static void checkSubmissionExistence(String projectKey, String releaseName, Optional<Submission> submissionOpt) {
    checkState(submissionOpt.isPresent(), "Failed to find submission for release '%s' and project '%'", releaseName,
        projectKey);
  }

}
