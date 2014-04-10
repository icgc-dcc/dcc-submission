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
package org.icgc.dcc.submission.fs;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.icgc.dcc.submission.core.util.Constants.Authorizations_ADMIN_ROLE;
import static org.icgc.dcc.submission.shiro.AuthorizationPrivileges.projectViewPrivilege;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;

@Slf4j
@AllArgsConstructor
public class ReleaseFileSystem {

  /**
   * System files directory name.
   */
  public static final String SYSTEM_FILES_DIR_NAME = ".system";

  /**
   * Dependencies.
   */
  @NonNull
  private final DccFileSystem dccFileSystem;
  @NonNull
  private final Release release;
  private final Subject userSubject;

  public ReleaseFileSystem(DccFileSystem dccFilesystem, Release release) {
    this(dccFilesystem, release, null);
  }

  public SubmissionDirectory getSubmissionDirectory(@NonNull String projectKey) {
    val allowed = hasPrivileges(projectKey);
    if (!allowed) {
      throw new DccFileSystemException("User '%s' with principal '%s' does not have permission to access project '%s'",
          userSubject, userSubject == null ? null : userSubject.getPrincipal(), projectKey);
    }

    val optional = release.getSubmission(projectKey);
    if (!optional.isPresent()) {
      throw new ReleaseException("There is no project '%s' associated with release '%s'",
          projectKey, release.getName());
    }

    val submission = optional.get();
    return new SubmissionDirectory(dccFileSystem, this, release, projectKey, submission);
  }

  public void setUpNewReleaseFileSystem(
      String oldReleaseName, // Remove after DCC-1940
      String newReleaseName,
      @NonNull ReleaseFileSystem previous,
      @NonNull Iterable<String> signedOffProjectKeys,
      @NonNull Iterable<String> otherProjectKeys) {
    log.info("Setting up new release file system for: '{}'", newReleaseName);

    checkState(signedOffProjectKeys.iterator().hasNext() || otherProjectKeys.iterator().hasNext(),
        "There must be at least on project key to process");

    // Shorthands
    val fileSystem = dccFileSystem.getFileSystem();
    val next = this;

    dccFileSystem.createReleaseDirectory(newReleaseName);

    // Create empty dirs along with nested .validation
    dccFileSystem.createProjectDirectoryStructures(
        newReleaseName,
        newLinkedHashSet(signedOffProjectKeys));

    for (val otherProjectKey : otherProjectKeys) {
      // Move "releaseName/projectKey/"
      move(fileSystem,
          previous.getSubmissionDirectory(otherProjectKey).getSubmissionDirPath(),
          next.getSubmissionDirectory(otherProjectKey).getSubmissionDirPath());

      // Band-aid: see DCC-1940
      dccFileSystem.createProjectDirectory(oldReleaseName, otherProjectKey);
    }

    // Move "releaseName/projectKey/.system"
    moveSystemDir(previous, fileSystem, next);
  }

  public void resetValidationFolder(@NonNull String projectKey) {
    val validationStringPath = dccFileSystem.buildValidationDirStringPath(release.getName(), projectKey);
    dccFileSystem.removeDirIfExist(validationStringPath);
    dccFileSystem.createDirIfDoesNotExist(validationStringPath);
    log.info("Emptied directory '{}' for project '{}'", validationStringPath, projectKey);
  }

  public boolean isReadOnly() {
    return ReleaseState.COMPLETED == release.getState();
  }

  public DccFileSystem getDccFileSystem() {
    return dccFileSystem;
  }

  public Release getRelease() {
    return release;
  }

  public Path getReleaseDirectory() {
    return new Path(this.dccFileSystem.getRootStringPath(), this.release.getName());
  }

  protected Path getSystemDirPath() {
    return new Path(this.getReleaseDirectory(), SYSTEM_FILES_DIR_NAME);
  }

  public boolean isSystemDirectory(Path path) {
    return getSystemDirPath().getName().equals(path.getName());
  }

  public boolean isAdminUser() {
    return userSubject.hasRole(Authorizations_ADMIN_ROLE);
  }

  private boolean isApplication() {
    return userSubject == null;
  }

  private boolean hasPrivileges(String projectKey) {
    return isApplication() || userSubject.isPermitted(projectViewPrivilege(projectKey));
  }

  private static void moveSystemDir(ReleaseFileSystem previous, FileSystem fileSystem, ReleaseFileSystem next) {
    val sourceSystemDir = previous.getSystemDirPath();
    val targetSystemDir = next.getSystemDirPath();

    log.info("Creating '{}'", targetSystemDir);
    HadoopUtils.mkdirs(fileSystem, targetSystemDir.toString());

    val systemFilePaths = HadoopUtils.lsFile(fileSystem, sourceSystemDir);
    for (val sourceSystemFilePath : systemFilePaths) {
      val targetSystemFilePath = new Path(targetSystemDir, sourceSystemFilePath.getName());

      movePath(fileSystem, sourceSystemFilePath, targetSystemFilePath);
    }
  }

  @SneakyThrows
  private static void movePath(FileSystem fileSystem, Path source, Path target) {
    move(fileSystem, source.toString(), target.toString());
  }

  @SneakyThrows
  private static void move(FileSystem fileSystem, String source, String target) {
    try {
      log.info("Moving '{}' to '{}'...", source, target);
      HadoopUtils.mv(fileSystem, source, target);
    } catch (Throwable t) {
      log.error("Could not move '{}' to '{}': " + t, source, target);

      throw t;
    }
  }

}
