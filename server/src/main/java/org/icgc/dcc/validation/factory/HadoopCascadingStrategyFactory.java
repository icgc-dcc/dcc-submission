package org.icgc.dcc.validation.factory;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.validation.CascadingStrategy;
import org.icgc.dcc.validation.HadoopCascadingStrategy;

public class HadoopCascadingStrategyFactory implements CascadingStrategyFactory {

  @Override
  public CascadingStrategy get(FileSystem fileSystem, Path input, Path output) {
    return new HadoopCascadingStrategy(fileSystem, input, output);
  }

}
