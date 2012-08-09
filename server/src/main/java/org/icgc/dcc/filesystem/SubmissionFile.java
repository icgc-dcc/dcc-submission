package org.icgc.dcc.filesystem;

import java.util.Date;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;

/**
 * For serializing file data through the REST interface
 */
public class SubmissionFile {
  public String name;

  public Date lastUpdate;

  public long size;

  public SubmissionFile() {
  }

  public SubmissionFile(Path path, FileSystem fs) {
    this.name = path.getName();

    FileStatus fileStatus = HadoopUtils.getFileStatus(fs, path);
    this.lastUpdate = new Date(fileStatus.getModificationTime());
    this.size = fileStatus.getLen();
  }
}
