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
package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.shiro.AuthorizationPrivileges;
import org.icgc.dcc.web.Authorizations;
import org.mortbay.log.Log;

public class ReleaseFileSystem {

  private final DccFileSystem dccFileSystem;

  private final Release release;

  private final Subject userSubject;

  public static final String SYSTEM_FILES = "SystemFiles";

  public ReleaseFileSystem(DccFileSystem dccFilesystem, Release release, Subject subject) {
    super();

    checkArgument(dccFilesystem != null);
    checkArgument(release != null);

    this.dccFileSystem = dccFilesystem;
    this.release = release;
    this.userSubject = subject; // may be null
  }

  public ReleaseFileSystem(DccFileSystem dccFilesystem, Release release) {
    this(dccFilesystem, release, null);
  }

  public SubmissionDirectory getSubmissionDirectory(String projectKey) {
    checkNotNull(projectKey);
    checkSubmissionDirectory(projectKey); // also checks privileges
    Submission submission = release.getSubmission(projectKey);
    return new SubmissionDirectory(dccFileSystem, release, projectKey, submission);
  }

  private void checkSubmissionDirectory(String projectKey) {
    checkNotNull(projectKey);
    if (hasPrivileges(projectKey) == false) {
      throw new DccFileSystemException("User " + userSubject.getPrincipal()
          + " does not have permission to access project " + projectKey);
    }
    String projectStringPath = dccFileSystem.buildProjectStringPath(release, projectKey);
    boolean exists = HadoopUtils.checkExistence(dccFileSystem.getFileSystem(), projectStringPath);
    if (exists == false) {
      throw new DccFileSystemException("Release directory " + projectStringPath + " does not exist");
    }
  }

  public void moveFrom(ReleaseFileSystem previous, List<String> projectKeys) {
    FileSystem fileSystem = this.dccFileSystem.getFileSystem();

    for (String projectKey : projectKeys) {
      SubmissionDirectory previousSubmissionDirectory = previous.getSubmissionDirectory(projectKey);
      SubmissionDirectory newSubmissionDirectory = getSubmissionDirectory(projectKey);
      for (String filename : previousSubmissionDirectory.listFile()) {
        String origin = previousSubmissionDirectory.getDataFilePath(filename);
        String destination = newSubmissionDirectory.getDataFilePath(filename);
        Log.info("moving {} to {} ", origin, destination);
        HadoopUtils.mv(fileSystem, origin, destination);
      }
      // move .validation folder over
      HadoopUtils.mv(fileSystem, previousSubmissionDirectory.getValidationDirPath(),
          newSubmissionDirectory.getValidationDirPath());
    }

    // also move System Files from previous releases
    Path originDir = previous.getSystemDirectory();
    Path destinationDir = this.getSystemDirectory();
    HadoopUtils.mkdirs(fileSystem, destinationDir.toString());

    List<Path> files = HadoopUtils.lsFile(fileSystem, originDir);
    for (Path originFile : files) {
      Path destinationFile = new Path(destinationDir, originFile.getName());
      HadoopUtils.mv(fileSystem, originFile, destinationFile); // temporary solution until DCC-835 is done
    }
  }

  public void emptyValidationFolders() {
    for (String projectKey : release.getProjectKeys()) {
      resetValidationFolder(projectKey);
    }
  }

  public void resetValidationFolder(String projectKey) {
    String validationStringPath = this.dccFileSystem.buildValidationDirStringPath(release, projectKey);
    dccFileSystem.removeDirIfExist(validationStringPath);
    dccFileSystem.createDirIfDoesNotExist(validationStringPath);
    Log.info("emptied directory {} for project {} ", validationStringPath, projectKey);
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

  public Path getSystemDirectory() {
    return new Path(this.getReleaseDirectory(), ReleaseFileSystem.SYSTEM_FILES);
  }

  public boolean isSystemDirectory(Path path) {
    return this.getSystemDirectory().getName().equals(path.getName())
        && this.userSubject.hasRole(Authorizations.ADMIN_ROLE);
  }

  private boolean isApplication() {
    return this.userSubject == null;
  }

  private boolean hasPrivileges(String projectKey) {
    return isApplication() || this.userSubject.isPermitted(AuthorizationPrivileges.projectViewPrivilege(projectKey));
  }

}
