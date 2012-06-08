package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.hsqldb.lib.StringInputStream;
import org.icgc.dcc.config.ConfigConstants;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Projects;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.User;
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
  final FileSystem fileSystem;

  private final Config config;

  private final Projects projects;

  private final String root;

  @Inject
  public DccFileSystem(Config config, Projects projects, FileSystem fileSystem) {
    super();

    checkArgument(config != null);
    checkArgument(projects != null);
    checkArgument(fileSystem != null);

    this.config = config;
    this.projects = projects;
    this.fileSystem = fileSystem;

    // grab root directory
    this.root = this.config.getString(ConfigConstants.FS_ROOT_PARAMETER);
    checkArgument(this.root != null);

    log.info("use_hdfs = " + this.config.getBoolean(ConfigConstants.FS_USE_HDFS));
    log.info("fileSystem = " + this.fileSystem.getClass().getSimpleName());
    log.info("home = " + this.fileSystem.getHomeDirectory());
    log.info("wd = " + this.fileSystem.getWorkingDirectory());
    log.info("root = " + this.root);

    this.mkdirsRootDirectory();
  }

  // TODO: put as proper tests
  public void testIt() throws Exception {
    log.info("ls = " + HadoopUtils.toFilenameList(HadoopUtils.ls(this.fileSystem, this.root)));

    Release myRelease = new Release("ICGC4");

    User myUser = new User();
    myUser.setUsername("vegeta");

    Project myProject = new Project("dragon_balls_quest");
    myProject.setAccessionId("DBQ");

    this.ensureReleaseFilesystem(myRelease);

    ReleaseFileSystem myReleaseFilesystem = this.getReleaseFilesystem(myRelease, myUser);
    log.info("release file system = " + myReleaseFilesystem);

    Iterable<SubmissionDirectory> mySubmissionDirectoryList = myReleaseFilesystem.listSubmissionDirectory();
    log.info("mySubmissionDirectoryList # = " + ((ArrayList<SubmissionDirectory>) mySubmissionDirectoryList).size());
    log.info("read only = " + myReleaseFilesystem.isReadOnly());

    SubmissionDirectory mySubmissionDirectory = myReleaseFilesystem.getSubmissionDirectory(myProject);

    String filename1 = "cnsm__bla__bla__p__bla__bla.tsv";
    InputStream in1 = new StringInputStream("header1\theader2\theader3\na\tb\tc\nd\te\tf\tg\n");
    String filepath1 = mySubmissionDirectory.addFile(filename1, in1);
    HadoopUtils.checkExistence(this.fileSystem, filepath1);
    log.info("added file = " + filepath1);

    String filename2 = "cnsm__bla__bla__s__bla__bla.tsv";
    InputStream in2 = new StringInputStream("header9\theader8\theader7\nz\tb\ty\nx\tw\tv\tu\n");
    String filepath2 = mySubmissionDirectory.addFile(filename2, in2);
    HadoopUtils.checkExistence(this.fileSystem, filepath2);
    log.info("added file = " + filepath2);

    Iterable<String> fileList1 = mySubmissionDirectory.listFile();
    log.info("ls1 = " + fileList1);

    Iterable<String> fileList2 = mySubmissionDirectory.listFile(Pattern.compile(".*__p__.*"));
    log.info("ls2 = " + fileList2);

    mySubmissionDirectory.deleteFile(filename1);

    HadoopUtils.rmr(this.fileSystem, this.root + "/" + myRelease.getName());
  }

  /**
   * Creates new user-tailored "view" of a given release filesystem. We may change that behavior later to not creating
   * it on the fly (for now we have very few users and don't plan on having millions ever).
   */
  public ReleaseFileSystem getReleaseFilesystem(Release release, User user) {
    return new ReleaseFileSystem(this, this.projects, release, user);
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
    String buildReleaseStringPath = this.buildReleaseStringPath(release);

    // check for pre-existence
    boolean exists = HadoopUtils.checkExistence(this.fileSystem, buildReleaseStringPath);
    if(exists) {
      throw new DccFileSystemException(this.fileSystem, this.root);
    }

    // create corresponding release directory
    HadoopUtils.mkdir(this.fileSystem, buildReleaseStringPath);
    checkState(HadoopUtils.checkExistence(this.fileSystem, buildReleaseStringPath)); // TODO: better assert
                                                                                     // somewhere?
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
      HadoopUtils.mkdir(this.fileSystem, projectStringPath);
      checkState(HadoopUtils.checkExistence(this.fileSystem, projectStringPath));
    }
  }

  public String buildReleaseStringPath(Release release) {
    checkArgument(release != null);
    return concatPath(this.root, release.getName());
  }

  public String buildProjectStringPath(Release release, Project project) {
    checkArgument(project != null);
    return concatPath(this.buildReleaseStringPath(release), project.getAccessionId());
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
    boolean rootExists = HadoopUtils.checkExistence(this.fileSystem, this.root);
    if(!rootExists) {
      log.info(this.root + " does not exist");
      HadoopUtils.mkdir(this.fileSystem, this.root);
      log.info("created " + this.root);
    }
  }

}
