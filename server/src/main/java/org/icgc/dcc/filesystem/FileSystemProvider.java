package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;

class FileSystemProvider implements Provider<FileSystem> {

  private static final Logger log = LoggerFactory.getLogger(FileSystemProvider.class);

  private final Configuration configuration; // hadoop's

  private final Config config; // typesafe's

  @Inject
  FileSystemProvider(Config config, Configuration hadoopConfig) {
    checkArgument(config != null);
    checkArgument(hadoopConfig != null);
    this.config = config;
    this.configuration = hadoopConfig;
  }

  @Override
  public FileSystem get() {
    String fsUrl = this.config.getString(FsConfig.FS_URL);
    this.configuration.set(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY, fsUrl);
    try {
      log.info("configuration = " + HadoopUtils.getConfigurationDescription(this.configuration)); // TODO formatting?
      return FileSystem.get(this.configuration);
    } catch(IOException e) {
      throw new RuntimeException(e);// TODO: better
    }
  }
}
