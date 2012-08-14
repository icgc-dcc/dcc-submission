package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.validation.factory.CascadingStrategyFactory;
import org.icgc.dcc.validation.factory.HadoopCascadingStrategyFactory;
import org.icgc.dcc.validation.factory.LocalCascadingStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;

public class CascadingStrategyFactoryProvider implements Provider<CascadingStrategyFactory> {

  private static final Logger log = LoggerFactory.getLogger(CascadingStrategyFactoryProvider.class);

  private final Config config;

  private final FileSystem fs;

  static final String FS_URL = "fs.url";

  @Inject
  CascadingStrategyFactoryProvider(Config config, FileSystem fs) {
    checkArgument(config != null);
    checkArgument(fs != null);
    this.config = config;
    this.fs = fs;
  }

  @Override
  public CascadingStrategyFactory get() {
    String fsUrl = this.config.getString(FS_URL);

    if(fsUrl.startsWith("file://")) {
      log.info("System configured for local filesystem");
      return new LocalCascadingStrategyFactory();
    } else if(fsUrl.startsWith("hdfs://")) {
      log.info("System configured for Hadoop filesystem");
      return new HadoopCascadingStrategyFactory(fs);
    } else {
      throw new RuntimeException("Unknown URI type: " + fsUrl + ". Expected file:// or hdfs://");
    }
  }

}
