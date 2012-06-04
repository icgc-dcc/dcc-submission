package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.typesafe.config.Config;

public class DccFilesystem {

  private static final Logger log = LoggerFactory.getLogger(DccFilesystem.class);

  private final Config config;

  @Inject
  public DccFilesystem(Config config) {
    checkArgument(config != null);
    this.config = config;
  }

  public void doIt() {
    // test can get some params
    log.info("use_hdfs = " + this.config.getBoolean("fs.use_hdfs"));
  }
}
