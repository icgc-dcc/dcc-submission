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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.toFilenameList;
import static org.icgc.dcc.submission.fs.FsConfig.FS_ROOT;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.release.model.Release;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.typesafe.config.Config;

@Slf4j
public class DccFileSystem {

  public static final String VALIDATION_DIRNAME = ".validation";

  /**
   * This is the only hadoop element in this class (everything else is handled in HadoopUtils)
   */
  private final FileSystem fileSystem;

  private final Config config;

  private final String rootStringPath;

  @Inject
  public DccFileSystem(Config config, FileSystem fileSystem) {
    super();

    checkArgument(config != null);
    checkArgument(fileSystem != null);

    this.config = config;
    this.fileSystem = fileSystem;

    // grab root directory
    this.rootStringPath = this.config.getString(FS_ROOT);
    checkState(this.rootStringPath != null);

    log.info("fileSystem = " + this.fileSystem.getClass().getSimpleName());
    log.info("rootStringPath = " + this.rootStringPath);
    log.info("home = " + this.fileSystem.getHomeDirectory());
    log.info("wd = " + this.fileSystem.getWorkingDirectory());

    this.mkdirsRootDirectory();
  }

  public FileSystem getFileSystem() {
    return this.fileSystem;
  }

  // TODO: for tests only (remove later?)
  public String getRootStringPath() {
    return this.rootStringPath;
  }

  /**
   * Creates new user-tailored "view" of a given release filesystem. We may change that behavior later to not creating
   * it on the fly (for now we have very few users and don't plan on having millions ever).
   */
  public ReleaseFileSystem getReleaseFilesystem(Release release, Subject subject) {
    return new ReleaseFileSystem(this, release, subject);
  }

  /**
   * Creates new project-tailored "view" of a given release filesystem. As a result, only a subset of the user-tailored
   * are actually accessible () We may change that behavior later to not creating it on the fly (for now we have very
   * few users and don't plan on having millions ever).
   */
  public ReleaseFileSystem getReleaseFilesystem(Release release) {
    return new ReleaseFileSystem(this, release);
  }

  /**
   * Ensures that the directory arborescence representing the given release exists, creates it if it does not.
   * 
   * @param release the new release
   */
  public void createInitialReleaseFilesystem(Release release, Set<String> projectKeyList) {
    String newReleaseName = release.getName();

    // create path for release
    val releaseStringPath = createReleaseDirectory(newReleaseName);
    createProjectDirectories(release.getName(), projectKeyList);

    // create system files for release directory
    ReleaseFileSystem releaseFS = this.getReleaseFilesystem(release);

    Path systemFilePath = releaseFS.getSystemDirectory();
    checkState(!HadoopUtils.checkExistence(this.fileSystem, systemFilePath.toString()));
    HadoopUtils.mkdirs(this.fileSystem, systemFilePath.toString());

    // log resulting sub-directories
    List<Path> lsAll = HadoopUtils.lsAll(this.fileSystem, new Path(releaseStringPath));
    log.info("ls {} = {}", releaseStringPath, toFilenameList(lsAll));
  }

  /**
   * TODO: move this to {@link ReleaseFileSystemTest}...
   */
  protected String createReleaseDirectory(String newReleaseName) {
    String releaseStringPath = this.buildReleaseStringPath(newReleaseName);
    log.info("Creating new release path: '{}'", releaseStringPath);

    checkState(!HadoopUtils.checkExistence(this.fileSystem, releaseStringPath),
        "Release directory already exists: '%s'", releaseStringPath);
    log.info("Creating filesystem for release: '{}'", newReleaseName);

    // create corresponding release directory
    HadoopUtils.mkdirs(this.fileSystem, releaseStringPath);

    return releaseStringPath;
  }

  /**
   * TODO: this is duplicate logic that belongs to {@link SubmissionDirectory}...
   */
  public String createProjectDirectoryStructure(String release, String projectKey) {
    checkArgument(release != null);
    checkArgument(projectKey != null);

    String projectStringPath = this.buildProjectStringPath(release, projectKey);
    log.info("Creating new directory: '%s'", projectStringPath);
    createDirIfDoesNotExist(projectStringPath); // TODO: change to error out if exists

    String validationStringPath = this.buildValidationDirStringPath(release, projectKey);
    log.info("Creating new directory: '%s'", validationStringPath);
    createDirIfDoesNotExist(validationStringPath); // TODO: change to error out if exists

    return projectStringPath;
  }

  /**
   * TODO: this is duplicate logic that belongs to {@link SubmissionDirectory}...
   */
  void createDirIfDoesNotExist(final String stringPath) {
    if (HadoopUtils.checkExistence(this.fileSystem, stringPath) == false) {
      HadoopUtils.mkdirs(this.fileSystem, stringPath);
      checkState(HadoopUtils.checkExistence(this.fileSystem, stringPath));
    }
  }

  /**
   * TODO: this is duplicate logic that belongs to {@link SubmissionDirectory}...
   */
  void removeDirIfExist(final String stringPath) {
    if (HadoopUtils.checkExistence(this.fileSystem, stringPath)) {
      HadoopUtils.rmr(this.fileSystem, stringPath);
      checkState(HadoopUtils.checkExistence(this.fileSystem, stringPath) == false);
    }
  }

  public String buildReleaseStringPath(String release) {
    checkArgument(release != null);
    return concatPath(this.rootStringPath, release);
  }

  public String buildProjectStringPath(String release, String projectKey) {
    checkArgument(projectKey != null);
    return concatPath(this.buildReleaseStringPath(release), projectKey);
  }

  public String buildFileStringPath(String release, String projectKey, String filename) {
    checkArgument(filename != null);
    return concatPath(this.buildProjectStringPath(release, projectKey), filename);
  }

  public String buildValidationDirStringPath(String release, String projectKey) {
    return concatPath(this.buildProjectStringPath(release, projectKey), VALIDATION_DIRNAME);
  }

  /**
   * TODO: move this to {@link ReleaseFileSystemTest}...
   */
  protected void createProjectDirectories(String release,
      @NonNull
      Set<String> projectKeys) {

    // Create sub-directory for each project
    log.info("# of projects = " + projectKeys.size());
    for (String projectKey : projectKeys) {
      this.createProjectDirectoryStructure(release, projectKey);
    }
  }

  private String concatPath(String... parts) {
    return Joiner.on(Path.SEPARATOR_CHAR).join(parts);
  }

  /**
   * Creates root directory if it does not exist
   */
  private void mkdirsRootDirectory() {
    // create root dir if it does not exist
    boolean rootExists = HadoopUtils.checkExistence(this.fileSystem, this.rootStringPath);
    if (!rootExists) {
      log.info(this.rootStringPath + " does not exist");
      HadoopUtils.mkdirs(this.fileSystem, this.rootStringPath);
      log.info("created " + this.rootStringPath);
    }
  }

  /**
   * @param filePath
   * @throws IOException
   */
  public DataInputStream open(String filePathname) throws IOException {
    checkArgument(filePathname != null);
    return fileSystem.open(new Path(filePathname));
  }

}
