package org.icgc.dcc.filesystem;

import java.util.Date;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;

/**
 * For serializing file data through the REST interface
 */
public class SubmissionFile {
  private final String name;

  private final Date lastUpdate;

  private final long size;

  private String matchedSchemaName;

  @JsonCreator
  public SubmissionFile(@JsonProperty("name") String name, @JsonProperty("lastUpdate") Date lastUpdate,
      @JsonProperty("size") long size, @JsonProperty("schema") String schema) {
    this.name = name;
    this.lastUpdate = lastUpdate;
    this.size = size;
    this.matchedSchemaName = schema;
  }

  public SubmissionFile(Path path, FileSystem fs, Dictionary dict) {
    this.name = path.getName();

    FileStatus fileStatus = HadoopUtils.getFileStatus(fs, path);
    this.lastUpdate = new Date(fileStatus.getModificationTime());
    this.size = fileStatus.getLen();

    this.matchedSchemaName = null;
    for(FileSchema schema : dict.getFiles()) {
      if(Pattern.matches(schema.getPattern(), this.name)) {
        this.matchedSchemaName = schema.getName();
        break;
      }
    }
  }

  public String getName() {
    return name;
  }

  public Date getLastUpdate() {
    return lastUpdate;
  }

  public long getSize() {
    return size;
  }

  public String getMatchedSchemaName() {
    return matchedSchemaName;
  }
}
