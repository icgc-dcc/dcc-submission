package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;

public class SubmissionDirectory {

  private final DccFileSystem dccFileSystem;

  private final Release release;

  private final Project project;

  private final Submission submission;

  public SubmissionDirectory(DccFileSystem dccFileSystem, Release release, Project project, Submission submission) {
    super();

    checkArgument(dccFileSystem != null);
    checkArgument(release != null);
    checkArgument(project != null);
    checkArgument(submission != null);

    this.dccFileSystem = dccFileSystem;
    this.release = release;
    this.project = project;
    this.submission = submission;
  }

  /**
   * (non-recursive) TODO: confirm
   */
  public Iterable<String> listFile(Pattern pattern) {
    String submissionDirectoryPath = this.dccFileSystem.buildProjectStringPath(this.release, this.project);
    checkArgument(submissionDirectoryPath != null);
    List<Path> pathList = HadoopUtils.ls(this.dccFileSystem.getFileSystem(), submissionDirectoryPath, pattern);
    return HadoopUtils.toFilenameList(pathList);
  }

  public Iterable<String> listFile() {
    return this.listFile(null);
  }

  public String addFile(String filename, InputStream data) {
    String filepath = this.dccFileSystem.buildFilepath(this.release, this.project, filename);
    HadoopUtils.touch(this.dccFileSystem.getFileSystem(), filepath, data);
    return filepath;
  }

  public String deleteFile(String filename) {
    String filepath = this.dccFileSystem.buildFilepath(this.release, this.project, filename);
    HadoopUtils.rm(this.dccFileSystem.getFileSystem(), filepath);
    return filepath;
  }

  public boolean isReadOnly() {
    SubmissionState state = this.submission.getState();
    return this.release.getState() == ReleaseState.COMPLETED//
        || state == SubmissionState.QUEUED || state == SubmissionState.SIGNED_OFF;
  }

  public String getProjectKey() {
    return this.project.getProjectKey();
  }
}
