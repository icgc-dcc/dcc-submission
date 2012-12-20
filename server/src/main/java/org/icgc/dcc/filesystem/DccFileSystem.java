package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Set;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.release.model.Release;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.typesafe.config.Config;

public class DccFileSystem {

  private static final Logger log = LoggerFactory.getLogger(DccFileSystem.class);

  public static final String VALIDATION_DIRNAME = ".validation";

  public static final String RELEASE_DIRNAME = ".release";

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
    this.rootStringPath = this.config.getString(FsConfig.FS_ROOT);
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
  public void ensureReleaseFilesystem(Release release, Set<String> projectKeyList) {

    // create path for release
    String releaseStringPath = this.buildReleaseStringPath(release);
    log.info("release path = " + releaseStringPath);

    // check for pre-existence
    boolean exists = HadoopUtils.checkExistence(this.fileSystem, releaseStringPath);
    if(exists) {
      log.info("filesystem for release " + release.getName() + " already exists");
      ensureSubmissionDirectories(release, projectKeyList);
    } else {
      log.info("creating filesystem for release " + release.getName());
      this.createReleaseFilesystem(release, projectKeyList);
    }

    // log resulting sub-directories
    log.info("ls " + releaseStringPath + " = "
        + HadoopUtils.toFilenameList(HadoopUtils.lsAll(this.fileSystem, releaseStringPath)));
  }

  /**
   * Creates the directory arborescence representing the given release.
   * 
   * @param release the new release
   */
  public void createReleaseFilesystem(Release release, Set<String> projectKeyList) {// TODO: make private?
    String releaseStringPath = this.buildReleaseStringPath(release);

    // check for pre-existence (at this point we expect it not to)
    boolean exists = HadoopUtils.checkExistence(this.fileSystem, releaseStringPath);
    if(exists) {
      throw new DccFileSystemException("release directory " + releaseStringPath + " already exists");
    }

    // create corresponding release directory
    HadoopUtils.mkdirs(this.fileSystem, releaseStringPath);
    ensureSubmissionDirectories(release, projectKeyList);

    // create system files for release directory
    ReleaseFileSystem releaseFS = this.getReleaseFilesystem(release);
    Path systemFilePath = releaseFS.getSystemDirectory();
    exists = HadoopUtils.checkExistence(this.fileSystem, systemFilePath.toString());
    if(exists == false) {
      HadoopUtils.mkdirs(this.fileSystem, systemFilePath.toString());
    }
  }

  public void mkdirProjectDirectory(Release release, String projectKey) {
    checkArgument(release != null);
    checkArgument(projectKey != null);

    String projectStringPath = this.buildProjectStringPath(release, projectKey);
    createDirIfDoesNotExist(projectStringPath);
    String validationStringPath = this.buildValidationDirStringPath(release, projectKey);
    createDirIfDoesNotExist(validationStringPath);

    log.info("\t" + "project path = " + projectStringPath);
  }

  void createDirIfDoesNotExist(final String stringPath) {
    if(HadoopUtils.checkExistence(this.fileSystem, stringPath) == false) {
      HadoopUtils.mkdirs(this.fileSystem, stringPath);
      checkState(HadoopUtils.checkExistence(this.fileSystem, stringPath));
    }
  }

  void removeDirIfExist(final String stringPath) {
    if(HadoopUtils.checkExistence(this.fileSystem, stringPath)) {
      HadoopUtils.rmr(this.fileSystem, stringPath);
      checkState(HadoopUtils.checkExistence(this.fileSystem, stringPath) == false);
    }
  }

  public String buildReleaseStringPath(Release release) {
    checkArgument(release != null);
    return concatPath(this.rootStringPath, release.getName());
  }

  public String buildProjectStringPath(Release release, String projectKey) {
    checkArgument(projectKey != null);
    return concatPath(this.buildReleaseStringPath(release), projectKey);
  }

  public String buildFileStringPath(Release release, String projectKey, String filename) {
    checkArgument(filename != null);
    return concatPath(this.buildProjectStringPath(release, projectKey), filename);
  }

  public String buildValidationDirStringPath(Release release, String projectKey) {
    return concatPath(this.buildProjectStringPath(release, projectKey), VALIDATION_DIRNAME);
  }

  private void ensureSubmissionDirectories(Release release, Set<String> projectKeyList) {
    // create sub-directory for each project
    checkState(projectKeyList != null);
    log.info("# of projects = " + projectKeyList.size());
    for(String project : projectKeyList) {
      this.mkdirProjectDirectory(release, project);
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
    if(!rootExists) {
      log.info(this.rootStringPath + " does not exist");
      HadoopUtils.mkdirs(this.fileSystem, this.rootStringPath);
      log.info("created " + this.rootStringPath);
    }
  }

}
