package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Projects;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.User;
import org.icgc.dcc.service.ReleaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.typesafe.config.Config;

public class DccFileSystem {

  private static final Logger log = LoggerFactory.getLogger(DccFileSystem.class);

  /**
   * This is the only hadoop element in this class (everything else is handled in HadoopUtils)
   */
  private final FileSystem fileSystem;

  private final Config config;

  private final ReleaseService releases;

  private final Projects projects;

  private final String rootStringPath;

  @Inject
  public DccFileSystem(Config config, ReleaseService releases, Projects projects, FileSystem fileSystem) {
    super();

    checkArgument(config != null);
    checkArgument(releases != null);
    checkArgument(projects != null);
    checkArgument(fileSystem != null);

    this.config = config;
    this.releases = releases;
    this.projects = projects;
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
  public ReleaseFileSystem getReleaseFilesystem(Release release, User user) {
    return new ReleaseFileSystem(this, this.releases, this.projects, release, user);
  }

  /**
   * Ensures that the directory arborescence representing the given release exists, creates it if it does not.
   * 
   * @param release the new release
   */
  public void ensureReleaseFilesystem(Release release) {

    // create path for release
    String releaseStringPath = this.buildReleaseStringPath(release);
    log.info("release path = " + releaseStringPath);

    // check for pre-existence
    boolean exists = HadoopUtils.checkExistence(this.fileSystem, releaseStringPath);
    if(exists) {
      log.info("filesystem for release " + release.getName() + " already exists");
      ensureSubmissionDirectories(release);
    } else {
      log.info("creating filesystem for release " + release.getName());
      this.createReleaseFilesystem(release);
    }

    // log resulting sub-directories
    log.info("ls " + releaseStringPath + " = "
        + HadoopUtils.toFilenameList(HadoopUtils.ls(this.fileSystem, releaseStringPath)));
  }

  /**
   * Creates the directory arborescence representing the given release.
   * 
   * @param release the new release
   */
  public void createReleaseFilesystem(Release release) {// TODO: make private?
    String releaseStringPath = this.buildReleaseStringPath(release);

    // check for pre-existence (at this point we expect it not to)
    boolean exists = HadoopUtils.checkExistence(this.fileSystem, releaseStringPath);
    if(exists) {
      throw new DccFileSystemException("release directory " + releaseStringPath + " already exists");
    }

    // create corresponding release directory
    HadoopUtils.mkdirs(this.fileSystem, releaseStringPath);
    ensureSubmissionDirectories(release);
  }

  public void mkdirProjectDirectory(Release release, Project project) {
    checkArgument(release != null);
    checkArgument(project != null);
    // create path for project within the release
    String projectStringPath = this.buildProjectStringPath(release, project);
    log.info("\t" + "project path = " + projectStringPath);
    if(HadoopUtils.checkExistence(this.fileSystem, projectStringPath) == false) {
      // create corresponding project directory
      HadoopUtils.mkdirs(this.fileSystem, projectStringPath);
      checkState(HadoopUtils.checkExistence(this.fileSystem, projectStringPath));
    }
  }

  public String buildReleaseStringPath(Release release) {
    checkArgument(release != null);
    return concatPath(this.rootStringPath, release.getName());
  }

  public String buildProjectStringPath(Release release, Project project) {
    checkArgument(project != null);
    return concatPath(this.buildReleaseStringPath(release), project.getProjectKey());
  }

  public String buildFilepath(Release release, Project project, String filename) {
    checkArgument(filename != null);
    return concatPath(this.buildProjectStringPath(release, project), filename);
  }

  private void ensureSubmissionDirectories(Release release) {
    // create sub-directory for each project
    List<Project> projectList = this.projects.getProjects();
    checkState(projectList != null);
    log.info("# of projects = " + projectList.size());
    for(Project project : projectList) {
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
