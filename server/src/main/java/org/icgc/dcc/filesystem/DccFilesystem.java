package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.config.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.typesafe.config.Config;

public class DccFilesystem {

  private static final Logger log = LoggerFactory.getLogger(DccFilesystem.class);

  private final Config config;

  private final FileSystem fileSystem;

  @Inject
  public DccFilesystem(Config config, FileSystem fileSystem) {
    checkArgument(config != null);
    checkArgument(fileSystem != null);
    this.config = config;
    this.fileSystem = fileSystem;
  }

  public void doIt() throws Exception {
    // dummy
    log.info("use_hdfs = " + this.config.getBoolean(ConfigConstants.FS_USE_HDFS));
    log.info("fileSystem = " + this.fileSystem.getClass().getSimpleName());

    log.info("home = " + this.fileSystem.getHomeDirectory());
    log.info("wd = " + this.fileSystem.getWorkingDirectory());
    String root = this.config.getString(ConfigConstants.FS_ROOT_PARAMETER);
    checkArgument(root != null);
    log.info("root = " + root);

    Path rootPath = new Path(root);
    boolean rootExists = this.fileSystem.exists(rootPath);
    if(!rootExists) {
      throw new RuntimeException(root + " does not exist");
    }

    FileStatus[] listStatus = this.fileSystem.listStatus(rootPath); // non-recursive
    List<String> ls = new ArrayList<String>();
    for(FileStatus fileStatus : listStatus) {
      String name = fileStatus.getPath().getName();
      ls.add(name);
    }
    log.info("ls = " + ls);
  }
}
