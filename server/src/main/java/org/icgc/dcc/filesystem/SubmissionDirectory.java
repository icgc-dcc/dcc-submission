package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;

public class SubmissionDirectory {

  private final DccFileSystem dccFileSystem;

  private final Release release;

  private final Project project;

  private final String submissionDirectoryPath;

  public SubmissionDirectory(DccFileSystem dccFileSystem, Release release, Project project) {
    super();

    checkArgument(dccFileSystem != null);
    checkArgument(release != null);
    checkArgument(project != null);

    this.dccFileSystem = dccFileSystem;
    this.release = release;
    this.project = project;

    this.submissionDirectoryPath = this.dccFileSystem.buildProjectStringPath(this.release, this.project);
    checkState(this.submissionDirectoryPath != null);
  }

  /**
   * (non-recursive) TODO: confirm
   */
  public Iterable<String> listFile(Pattern pattern) {
    List<Path> pathList = HadoopUtils.ls(this.dccFileSystem.getFileSystem(), this.submissionDirectoryPath, pattern);
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
    boolean readOnlyRelease = ReleaseState.COMPLETED.equals(this.release.getState());
    boolean validating = false;// TODO: ?
    return readOnlyRelease || validating;
  }

}
