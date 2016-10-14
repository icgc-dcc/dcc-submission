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
package org.icgc.dcc.submission.server.sftp;

import static com.google.common.base.Optional.fromNullable;
import static org.icgc.dcc.submission.fs.SubmissionFileEventType.FILE_CREATED;
import static org.icgc.dcc.submission.fs.SubmissionFileEventType.FILE_REMOVED;
import static org.icgc.dcc.submission.fs.SubmissionFileEventType.FILE_RENAMED;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.common.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.core.security.Authorizations;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.fs.SubmissionFile;
import org.icgc.dcc.submission.fs.SubmissionFileEvent;
import org.icgc.dcc.submission.fs.SubmissionFileRenamedEvent;
import org.icgc.dcc.submission.fs.SubmissionFileSystem;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.server.service.MailService;
import org.icgc.dcc.submission.server.service.ProjectService;
import org.icgc.dcc.submission.server.service.ReleaseService;
import org.icgc.dcc.submission.server.service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

/**
 * "Encapsulated Context Object" class that insulates and decouples the SFTP subsystem from DCC file system
 * abstractions. This is very similar in purpose to Hadoop's new API "Context Object for Mapper and Reducer".
 * <p>
 * Note that there still remain some accessors that should be removed as they violate the encapsulation. In this sense,
 * this should be considered a "Parameter Object" transitioning to a "Encapsulated Context Object".
 * 
 * @see http://www.two-sdg.demon.co.uk/curbralan/papers/europlop/ContextEncapsulation.pdf
 * @see http://www.allankelly.net/static/patterns/encapsulatecontext.pdf
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SftpContext {

  /**
   * Encapsulated context.
   */
  @NonNull
  private final SubmissionFileSystem fs;
  @NonNull
  private final ReleaseService releaseService;
  @NonNull
  private final SubmissionService submissionService;
  @NonNull
  private final ProjectService projectService;
  @NonNull
  private final AuthenticationManager authenticator;
  @NonNull
  private final MailService mailService;

  public List<String> getUserProjectKeys(Authentication authentication) {
    val projectKeys = Lists.<String> newArrayList();
    for (val project : projectService.getProjects()) {
      if (Authorizations.hasSpecificProjectPrivilege(authentication, project.getKey())) {
        projectKeys.add(project.getKey());
      }
    }

    return projectKeys;
  }

  // TODO: This should not be needed once the other todos are addressed
  public Release getNextRelease() {
    return releaseService.getNextRelease();
  }

  public String getNextReleaseName() {
    return getNextRelease().getName();
  }

  // TODO: Return Paths or Strings and nothing in org.dcc.filesystem.*
  public ReleaseFileSystem getReleaseFileSystem(Authentication authentication) {
    val nextRelease = getNextRelease();
    val submissions = submissionService.findSubmissionsByProjectKey(nextRelease.getName());

    return fs.getReleaseFilesystem(nextRelease, submissions, authentication);
  }

  public FileSystem getFileSystem() {
    return fs.getFileSystem();
  }

  public boolean isSystemDirectory(Path path, Authentication authentication) {
    return getReleaseFileSystem(authentication).isSystemDirectory(path);
  }

  public boolean isAdminUser(Authentication authentication) {
    return Authorizations.isAdmin(authentication);
  }

  public SubmissionFile getSubmissionFile(@NonNull Path path) throws IOException {
    return getSubmissionFile(releaseService.getNextDictionary(), path);
  }

  public SubmissionDirectory getSubmissionDirectory(String projectKey, Authentication authentication) {
    return getReleaseFileSystem(authentication).getSubmissionDirectory(projectKey);
  }

  public Path getReleasePath() {
    String releasePath = fs.buildReleaseStringPath(getNextRelease().getName());
    return new Path(releasePath);
  }

  public void registerReferenceChange() {
    releaseService.resetSubmissions();
  }

  public void registerSubmissionEvent(@NonNull String projectKey, @NonNull SubmissionFileEvent event,
      Authentication authentication) {
    val user = authentication.getName();
    val fileName = event.getFile().getName();

    if (event.getType() == FILE_CREATED) {
      log.info("'{}' finished transferring file '{}'", user, fileName);
      mailService.sendFileTransferred(user, event.getFile().getName());
    } else if (event.getType() == FILE_RENAMED) {
      val newFileName = ((SubmissionFileRenamedEvent) event).getNewFile().getName();
      log.info("'{}' renamed  file '{}' to '{}'", new Object[] { user, fileName, newFileName });
      mailService.sendFileRenamed(user, fileName, newFileName);
    } else if (event.getType() == FILE_REMOVED) {
      log.info("'{}' removed  file '{}'", user, fileName);
      mailService.sendFileRemoved(user, event.getFile().getName());
    }

    releaseService.modifySubmission(getNextReleaseName(), projectKey, event);
  }

  // TODO: Duplicated code with ReleaseService
  private SubmissionFile getSubmissionFile(Dictionary dictionary, Path filePath) throws IOException {
    val fileName = filePath.getName();
    val fileStatus = HadoopUtils.getFileStatus(fs.getFileSystem(), filePath).get();
    val fileLastUpdate = new Date(fileStatus.getModificationTime());
    val fileSize = fileStatus.getLen();
    val fileType = getSubmissionFileType(dictionary, filePath).orNull();

    return new SubmissionFile(fileName, fileLastUpdate, fileSize, fileType, false);
  }

  // TODO: Duplicated code with ReleaseService
  private Optional<FileType> getSubmissionFileType(Dictionary dictionary, Path filePath) {
    val fileName = filePath.getName();
    val fileSchema = dictionary.getFileSchemaByFileName(fileName);

    return fromNullable(fileSchema.isPresent() ? fileSchema.get().getFileType() : null);
  }

}
