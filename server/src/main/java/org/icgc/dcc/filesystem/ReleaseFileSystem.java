package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.sshd.server.SshFile;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.User;
import org.icgc.dcc.service.ProjectService;
import org.icgc.dcc.service.ReleaseService;
import org.icgc.dcc.sftp.HdfsSshDir;
import org.icgc.dcc.sftp.HdfsSshFile;

public class ReleaseFileSystem {

  private final DccFileSystem dccFileSystem;

  private final ProjectService projects;

  private final ReleaseService releases;

  private final Release release;

  private final User user;

  public ReleaseFileSystem(DccFileSystem dccFilesystem, ReleaseService releases, ProjectService projects,
      Release release, User user) {
    super();

    checkArgument(dccFilesystem != null);
    checkArgument(releases != null);
    checkArgument(projects != null);
    checkArgument(release != null);
    checkArgument(user != null);

    this.dccFileSystem = dccFilesystem;
    this.releases = releases;
    this.projects = projects;
    this.release = release;
    this.user = user;
  }

  /**
   * Lists all the submission directories for release/user combination
   */
  public Iterable<SubmissionDirectory> listSubmissionDirectory() {
    List<SubmissionDirectory> submissionDirectoryList = new ArrayList<SubmissionDirectory>();

    List<Project> projectList = this.projects.getProjects();
    for(Project project : projectList) {
      boolean hasUser = project.hasUser(this.user.getName());
      if(hasUser) {// TODO: use guava instead?
        SubmissionDirectory submissionDirectory = this.getSubmissionDirectory(project);
        submissionDirectoryList.add(submissionDirectory);
      }
    }

    return submissionDirectoryList;
  }

  public SubmissionDirectory getSubmissionDirectory(Project project) {
    checkSubmissionDirectory(project);
    Submission submission = this.releases.getSubmission(this.release.getName(), project.getProjectKey());
    return new SubmissionDirectory(this.dccFileSystem, this.release, project, submission);
  }

  public SubmissionDirectory getSubmissionDirectory(String projectKey) {
    return getSubmissionDirectory(projects.getProject(projectKey));
  }

  private void checkSubmissionDirectory(Project project) {
    if(project.hasUser(this.user.getName()) == false) {
      throw new DccFileSystemException("User " + this.user.getName() + " does not have permission to access project "
          + project);
    }
    String projectStringPath = this.dccFileSystem.buildProjectStringPath(this.release, project);
    boolean exists = HadoopUtils.checkExistence(this.dccFileSystem.getFileSystem(), projectStringPath);
    if(exists == false) {
      throw new DccFileSystemException("Release directory " + projectStringPath + " does not exist");
    }
  }

  public boolean isReadOnly() {
    return ReleaseState.COMPLETED == this.release.getState(); // TODO: better way?
  }

  public SshFile getSftpFile(String file) {
    Path originalFilePath = new Path(file);
    String absoluteFile;

    if(originalFilePath.depth() == 1) {
      Project project = projects.getProject(originalFilePath.getName());
      SubmissionDirectory sd = getSubmissionDirectory(project);
      absoluteFile = this.dccFileSystem.buildProjectStringPath(release, project);
      return new HdfsSshDir(new Path(absoluteFile), this.dccFileSystem.getFileSystem(), sd, this);
    } else if(originalFilePath.depth() == 2) {
      Project project = projects.getProject(originalFilePath.getParent().getName());
      SubmissionDirectory sd = getSubmissionDirectory(project);
      absoluteFile = this.dccFileSystem.buildFilepath(release, project, originalFilePath.getName());
      return new HdfsSshFile(new Path(absoluteFile), this.dccFileSystem.getFileSystem(), sd, this);
    } else {
      throw new DccFileSystemException("Invalid file path: " + file);
    }
  }
}
