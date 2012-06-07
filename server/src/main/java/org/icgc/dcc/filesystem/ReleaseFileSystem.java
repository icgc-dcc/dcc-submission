package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Projects;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.User;

public class ReleaseFileSystem {

  private final DccFileSystem dccFileSystem;

  private final Projects projects;

  private final Release release;

  private final User user;

  public ReleaseFileSystem(DccFileSystem dccFilesystem, Projects projects, Release release, User user) {
    super();

    checkArgument(dccFilesystem != null);
    checkArgument(projects != null);
    checkArgument(release != null);
    checkArgument(user != null);

    this.dccFileSystem = dccFilesystem;
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
      if(hasUser) {
        SubmissionDirectory submissionDirectory = this.getSubmissionDirectory(project);
        submissionDirectoryList.add(submissionDirectory);
      }
    }

    return submissionDirectoryList;
  }

  public SubmissionDirectory getSubmissionDirectory(Project project) {
    return new SubmissionDirectory(this.dccFileSystem, this.release, project);
  }

  public boolean isReadOnly() {
    return ReleaseState.COMPLETED.equals(this.release.getState()); // TODO: better way?
  }
}
