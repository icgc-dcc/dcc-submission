package org.icgc.dcc.validation.factory;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.validation.CascadingStrategy;
import org.icgc.dcc.validation.HadoopCascadingStrategy;

public class HadoopCascadingStrategyFactory implements CascadingStrategyFactory {

  private final FileSystem fileSystem;

  /**
   * @param fs
   */
  public HadoopCascadingStrategyFactory(FileSystem fs) {
    this.fileSystem = fs;
  }

  @Override
  public CascadingStrategy get(Path input, Path output, Path system) {
    return new HadoopCascadingStrategy(fileSystem, input, output, system);
  }

}
