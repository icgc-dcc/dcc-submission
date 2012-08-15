package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.mortbay.log.Log;

public class ReleaseFileSystem {

  private final DccFileSystem dccFileSystem;

  private final Release release;

  private final String username;

  public ReleaseFileSystem(DccFileSystem dccFilesystem, Release release, String username) {
    super();

    checkArgument(dccFilesystem != null);
    checkArgument(release != null);

    this.dccFileSystem = dccFilesystem;
    this.release = release;
    this.username = username; // may be null
  }

  public ReleaseFileSystem(DccFileSystem dccFilesystem, Release release) {
    this(dccFilesystem, release, null);
  }

  public SubmissionDirectory getSubmissionDirectory(Project project) {
    checkNotNull(project);
    checkSubmissionDirectory(project); // also checks privileges
    Submission submission = release.getSubmission(project.getKey());
    return new SubmissionDirectory(dccFileSystem, release, project, submission);
  }

  private void checkSubmissionDirectory(Project project) {
    checkNotNull(project);
    if(hasPrivileges(project) == false) {
      throw new DccFileSystemException("User " + username + " does not have permission to access project " + project);
    }
    String projectStringPath = dccFileSystem.buildProjectStringPath(release, project.getKey());
    boolean exists = HadoopUtils.checkExistence(dccFileSystem.getFileSystem(), projectStringPath);
    if(exists == false) {
      throw new DccFileSystemException("Release directory " + projectStringPath + " does not exist");
    }
  }

  public void moveFrom(ReleaseFileSystem previous, List<Project> projects) {
    for(Project project : projects) {
      SubmissionDirectory previousSubmissionDirectory = previous.getSubmissionDirectory(project);
      SubmissionDirectory newSubmissionDirectory = getSubmissionDirectory(project);
      for(String filename : previousSubmissionDirectory.listFile()) {
        String origin = previousSubmissionDirectory.getDataFilePath(filename);
        String destination = newSubmissionDirectory.getDataFilePath(filename);
        Log.info("moving {} to {} ", origin, destination);
        HadoopUtils.mv(this.dccFileSystem.getFileSystem(), origin, destination);
      }
      // move .validation folder over
      HadoopUtils.mv(this.dccFileSystem.getFileSystem(), previousSubmissionDirectory.getValidationDirPath(),
          newSubmissionDirectory.getValidationDirPath());
    }

    // also move System Files from previous releases
    HadoopUtils.createSymlink(this.dccFileSystem.getFileSystem(), previous.getSystemDirectory().toString(), this
        .getSystemDirectory().toString());
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
    return new Path(this.getReleaseDirectory(), "SystemFiles");
  }

  private boolean isApplication() {
    return username == null;
  }

  private boolean hasPrivileges(Project project) {
    return isApplication() || project.hasUser(username);
  }

}
