package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;

public class ReleaseFileSystem {

  private final DccFileSystem dccFileSystem;

  private final ProjectService projects;

  private final Release release;

  private final String username;

  public ReleaseFileSystem(DccFileSystem dccFilesystem, ProjectService projects, Release release, String username) {
    super();

    checkArgument(dccFilesystem != null);
    checkArgument(projects != null);
    checkArgument(release != null);

    this.dccFileSystem = dccFilesystem;
    this.projects = projects;
    this.release = release;
    this.username = username; // may be null
  }

  public ReleaseFileSystem(DccFileSystem dccFilesystem, ProjectService projects, Release release) {
    this(dccFilesystem, projects, release, null);
  }

  public SubmissionDirectory getSubmissionDirectory(Project project) {
    checkNotNull(project);
    checkSubmissionDirectory(project); // also checks privileges
    Submission submission = release.getSubmission(project.getKey());
    return new SubmissionDirectory(dccFileSystem, release, project, submission);
  }

  public SubmissionDirectory getSubmissionDirectory(String projectKey) {
    checkNotNull(projectKey);
    return getSubmissionDirectory(projects.getProject(projectKey));
  }

  private void checkSubmissionDirectory(Project project) {
    checkNotNull(project);
    if(hasPrivileges(project) == false) {
      throw new DccFileSystemException("User " + username + " does not have permission to access project " + project);
    }
    String projectStringPath = dccFileSystem.buildProjectStringPath(release, project);
    boolean exists = HadoopUtils.checkExistence(dccFileSystem.getFileSystem(), projectStringPath);
    if(exists == false) {
      throw new DccFileSystemException("Release directory " + projectStringPath + " does not exist");
    }
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

  private boolean isApplication() {
    return username == null;
  }

  private boolean hasPrivileges(Project project) {
    return isApplication() || project.hasUser(username);
  }
}
