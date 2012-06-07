package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.config.ConfigConstants;
import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Projects;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.typesafe.config.Config;

public class DccFilesystem {

  private static final Logger log = LoggerFactory.getLogger(DccFilesystem.class);

  private final Config config;

  private final FileSystem fileSystem;

  private final Projects projects;

  private String root;

  @Inject
  public DccFilesystem(Config config, Projects projects, FileSystem fileSystem) {

    checkArgument(config != null);
    checkArgument(projects != null);
    checkArgument(fileSystem != null);

    this.config = config;
    this.projects = projects;
    this.fileSystem = fileSystem;

    log.info("use_hdfs = " + this.config.getBoolean(ConfigConstants.FS_USE_HDFS));
    log.info("fileSystem = " + this.fileSystem.getClass().getSimpleName());
    log.info("home = " + this.fileSystem.getHomeDirectory());
    log.info("wd = " + this.fileSystem.getWorkingDirectory());

    this.mkdirsRootDirectory();
  }

  /**
   * Creates root directory if it does not exist
   */
  private void mkdirsRootDirectory() {

    // grab root directory
    this.root = this.config.getString(ConfigConstants.FS_ROOT_PARAMETER);
    checkArgument(this.root != null);
    log.info("root = " + this.root);

    // create root dir if it does not exist
    Path rootPath = new Path(this.root);
    boolean rootExists = this.checkExistence(rootPath);
    if(!rootExists) {
      log.info(this.root + " does not exist");
      this.createDirectory(rootPath);
      log.info("created " + this.root);
    }
  }

  // TODO: remove dummy
  public void doIt() throws Exception {
    Path rootPath = new Path(this.root);
    log.info("ls = " + this.listContent(rootPath));

    Release myRelease = new Release("ICGC4");

    User myUser = new User();
    myUser.setUsername("richard");

    this.createReleaseFilesystem(myRelease);
    log.info("release file system = " + this.getReleaseFilesystem(myRelease, myUser));

    try {
      this.fileSystem.delete(new Path(this.root + "/" + myRelease.getName()), true);
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates new user-tailored "view" of a given release filesystem. We may change that behavior later to not creating
   * it on the fly (for now we have very few users and don't plan on having millions ever).
   */
  public ReleaseFilesystem getReleaseFilesystem(Release release, User user) {
    return new ReleaseFilesystem(release, user);
  }

  /**
   * Creates the directory arborescence representing given release
   */
  public void createReleaseFilesystem(Release release) {

    // create path for release
    String releaseStringPath = this.buildReleaseStringPath(release);
    log.info("release path = " + releaseStringPath);
    Path releasePath = new Path(releaseStringPath);

    // check for pre-existence
    boolean exists = this.checkExistence(releasePath);
    if(exists) {
      throw new DccFileSystemException(this.fileSystem, this.root);
    }

    // create corresponding release directory
    this.createDirectory(releasePath);
    checkArgument(this.checkExistence(releasePath)); // TODO: better assert somewhere?

    // create sub-directory for each project
    List<Project> projectList = this.projects.getProjects();
    checkArgument(!projectList.isEmpty(), "project list cannot be empty");
    log.info("# of projects = " + projectList.size());
    for(Project project : projectList) {

      // create path for project within the release
      String projectStringPath = this.buildProjectStringPath(releaseStringPath, project);
      log.info("\t" + "project path = " + projectStringPath);
      Path projectPath = new Path(projectStringPath);
      checkArgument(!this.checkExistence(projectPath)); // theoretically can't really happen since we throw an exception
                                                        // if the release already exists and we assume all projects have
                                                        // unique names... TODO?

      // create corresponding project directory
      this.createDirectory(projectPath);
      checkArgument(this.checkExistence(projectPath));
    }

    // log resulting sub-directories
    log.info("ls " + releaseStringPath + " = " + this.listContent(releasePath));
  }

  private String buildReleaseStringPath(Release release) {
    return this.root + "/" + release.getName();
  }

  private String buildProjectStringPath(String releaseStringPath, Project project) {
    return releaseStringPath + "/" + project.getName();
  }

  // TODO: move those methods to HadoopUtils?

  private void createDirectory(Path path) {
    boolean mkdirs;
    try {
      mkdirs = this.fileSystem.mkdirs(path);
    } catch(IOException e) {
      throw new DccFileSystemException(this.fileSystem, this.root);
    }
    if(!mkdirs) {
      throw new DccFileSystemException(this.fileSystem, this.root);
    }
  }

  private boolean checkExistence(Path releasePath) {
    boolean exists;
    try {
      exists = this.fileSystem.exists(releasePath);
    } catch(IOException e) {
      throw new DccFileSystemException(this.fileSystem, this.root);
    }
    return exists;
  }

  /**
   * non-recursively
   */
  private List<String> listContent(Path path) {
    FileStatus[] listStatus;
    try {
      listStatus = this.fileSystem.listStatus(path);
    } catch(IOException e) {
      throw new DccFileSystemException(this.fileSystem, this.root);
    }
    List<String> ls = new ArrayList<String>();
    for(FileStatus fileStatus : listStatus) {
      String name = fileStatus.getPath().getName();
      ls.add(name);
    }
    return ls;
  }
}
