package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.shiro.AuthorizationPrivileges;
import org.mortbay.log.Log;

public class ReleaseFileSystem {

  private final DccFileSystem dccFileSystem;

  private final Release release;

  private final Subject userSubject;

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

  public SubmissionDirectory getSubmissionDirectory(Project project) {
    checkNotNull(project);
    checkSubmissionDirectory(project); // also checks privileges
    Submission submission = release.getSubmission(project.getKey());
    return new SubmissionDirectory(dccFileSystem, release, project, submission);
  }

  private void checkSubmissionDirectory(Project project) {
    checkNotNull(project);
    if(hasPrivileges(project) == false) {
      throw new DccFileSystemException("User " + userSubject.getPrincipal()
          + " does not have permission to access project " + project);
    }
    String projectKey = project.getKey();
    String projectStringPath = dccFileSystem.buildProjectStringPath(release, projectKey);
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
    Path origin = previous.getSystemDirectory();
    Path destination = this.getSystemDirectory();
    HadoopUtils.mkdirs(this.dccFileSystem.getFileSystem(), destination.toString());

    List<Path> files = HadoopUtils.lsFile(this.dccFileSystem.getFileSystem(), origin.toString());
    for(Path file : files) {
      HadoopUtils.createSymlink(this.dccFileSystem.getFileSystem(), file, new Path(destination, file.getName()));
    }
  }

  public void emptyValidationFolders() {
    for(String projectKey : release.getProjectKeys()) {
      String validationStringPath = this.dccFileSystem.buildValidationDirStringPath(release, projectKey);
      dccFileSystem.removeDirIfExist(validationStringPath);
      dccFileSystem.createDirIfDoesNotExist(validationStringPath);
      Log.info("emptied directory {} for project {} ", validationStringPath, projectKey);
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

  public Path getReleaseDirectory() {
    return new Path(this.dccFileSystem.getRootStringPath(), this.release.getName());
  }

  public Path getSystemDirectory() {
    return new Path(this.getReleaseDirectory(), "SystemFiles");
  }

  public Boolean isSystemDirectory(Path path) {
    if(this.userSubject == null) {
      return this.getSystemDirectory().getName().equals(path.getName());
    } else {
      return this.getSystemDirectory().getName().equals(path.getName()) && this.userSubject.hasRole("admin");
    }
  }

  private boolean isApplication() {
    return this.userSubject == null;
  }

  private boolean hasPrivileges(Project project) {
    return isApplication()
        || this.userSubject.isPermitted(AuthorizationPrivileges.projectViewPrivilege(project.getKey()));
  }

}
