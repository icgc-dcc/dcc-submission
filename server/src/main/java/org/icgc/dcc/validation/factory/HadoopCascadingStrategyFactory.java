package org.icgc.dcc.validation.factory;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.validation.CascadingStrategy;
import org.icgc.dcc.validation.HadoopCascadingStrategy;

import com.typesafe.config.Config;

public class HadoopCascadingStrategyFactory implements CascadingStrategyFactory {

  private final FileSystem fileSystem;

  private final Config hadoopConfig;

  public HadoopCascadingStrategyFactory(Config hadoopConfig, FileSystem fs) {
    this.fileSystem = fs;
    this.hadoopConfig = hadoopConfig;
  }

  @Override
  public CascadingStrategy get(Path input, Path output, Path system) {
    return new HadoopCascadingStrategy(hadoopConfig, fileSystem, input, output, system);
  }

}
