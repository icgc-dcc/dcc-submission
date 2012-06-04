package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;

public class LocalFilesystem implements IFilesystem {

  private static final Logger log = LoggerFactory.getLogger(LocalFilesystem.class);

  private static final String FS_ROOT_PARAMETER = "fs.root";

  private final Config config;

  private final String rootDir;

  public LocalFilesystem(Config config) {
    checkArgument(config != null);
    this.config = config;

    this.rootDir = this.fetchRootDir();
  }

  private String fetchRootDir() {
    String rootDir = this.config.getString(FS_ROOT_PARAMETER);
    checkArgument(rootDir != null);
    log.info(FS_ROOT_PARAMETER + " = " + rootDir);

    File root = new File(rootDir);
    if(!root.exists()) {
      log.info(rootDir + " does not exist");
      log.info("creating " + rootDir);
      root.mkdir();
    } else if(!root.isDirectory()) {
      throw new RuntimeException(rootDir + " exists but is not a directory");
    }
    return rootDir;
  }

  @Override
  public List<String> ls() {
    return Arrays.asList(new File(this.rootDir).list());
  }
}
