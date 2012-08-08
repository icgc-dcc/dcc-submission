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
  public final String name;

  public final Date lastUpdate;

  public final long size;

  public SubmissionFile(String name, Date lastUpdate, long size) {
    this.name = name;
    this.lastUpdate = lastUpdate;
    this.size = size;
  }

  public SubmissionFile(Path path, FileSystem fs) {
    this.name = path.getName();

    FileStatus fileStatus = HadoopUtils.getFileStatus(fs, path);
    this.lastUpdate = new Date(fileStatus.getModificationTime());
    this.size = fileStatus.getLen();
  }
}
