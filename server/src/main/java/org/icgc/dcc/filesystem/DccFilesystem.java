package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.typesafe.config.Config;

public class DccFilesystem {

  private static final Logger log = LoggerFactory.getLogger(DccFilesystem.class);

  private final Config config;

  private final IFilesystem fileSystem;

  @Inject
  public DccFilesystem(Config config, IFilesystem fileSystem) {
    checkArgument(config != null);
    checkArgument(fileSystem != null);
    this.config = config;
    this.fileSystem = fileSystem;
  }

  public void doIt() {
    // dummy
    log.info("use_hdfs = " + this.config.getBoolean("fs.use_hdfs"));
    log.info("ls = \n" + this.fileSystem.ls());
  }
}
