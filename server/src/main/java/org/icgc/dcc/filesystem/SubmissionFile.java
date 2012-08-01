package org.icgc.dcc.filesystem;

import java.io.IOException;
import java.util.Date;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/*
 * For serializing file data through the REST interface
 */
public class SubmissionFile {
  public final String name;

  public final Date lastUpdate;

  public final long size;

  public SubmissionFile(Path path, FileSystem fs) throws IOException {
    this.name = path.getName();
    this.lastUpdate = new Date(fs.getFileStatus(path).getModificationTime());
    this.size = fs.getFileStatus(path).getLen();
  }
}
