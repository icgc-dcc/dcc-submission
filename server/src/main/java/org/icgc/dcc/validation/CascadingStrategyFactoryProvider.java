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

public class CascadingStrategyFactoryProvider implements Provider<CascadingStrategyFactory> {

  private static final Logger log = LoggerFactory.getLogger(CascadingStrategyFactoryProvider.class);

  private final FileSystem fs;

  @Inject
  CascadingStrategyFactoryProvider(FileSystem fs) {
    checkArgument(fs != null);
    this.fs = fs;
  }

  @Override
  public CascadingStrategyFactory get() {
    String fsUrl = fs.getScheme();

    if(fsUrl.equals("file")) {
      log.info("System configured for local filesystem");
      return new LocalCascadingStrategyFactory();
    } else if(fsUrl.equals("hdfs")) {
      log.info("System configured for Hadoop filesystem");
      return new HadoopCascadingStrategyFactory(fs);
    } else {
      throw new RuntimeException("Unknown file system type: " + fsUrl + ". Expected file or hdfs");
    }
  }

}
