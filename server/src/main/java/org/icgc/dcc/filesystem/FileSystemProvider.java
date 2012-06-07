package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.config.ConfigConstants;
import org.icgc.dcc.filesystem.hdfs.HadoopConstants;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;

public class FileSystemProvider implements Provider<FileSystem> {
  private static final Logger log = LoggerFactory.getLogger(FileSystemProvider.class);

  @Inject
  private final Configuration configuration = new Configuration(); // hadoop's

  @Inject
  private Config config; // typesafe's

  @Override
  public FileSystem get() {

    boolean useHdfs = this.config.getBoolean(ConfigConstants.FS_USE_HDFS);
    if(useHdfs) { // else will use default: "file:///" (read "file://" + "/")

      String host = this.config.getString(ConfigConstants.FS__HDFS__HOST);
      Integer port = this.config.getInt(ConfigConstants.FS__HDFS__PORT);

      checkArgument(host != null);
      checkArgument(port != null);
      // TODO: defaults?

      String hdfsURL = "hdfs://" + host + ":" + port;// TODO constants
      log.info("hdfs URL = " + hdfsURL);
      this.configuration.set(HadoopConstants.FS_DEFAULT_NAME__PROPERTY, hdfsURL);
    }

    FileSystem fileSystem = null;
    try {
      log.info("configuration = " + HadoopUtils.getConfigurationDescription(this.configuration)); // TODO formatting?
      fileSystem = FileSystem.get(this.configuration);
    } catch(IOException e) {
      throw new RuntimeException(e);// TODO: better
    }
    return fileSystem;
  }
}
