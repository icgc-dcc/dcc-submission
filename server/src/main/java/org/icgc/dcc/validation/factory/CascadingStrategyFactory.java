package org.icgc.dcc.validation.factory;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.validation.CascadingStrategy;

public interface CascadingStrategyFactory {
  public CascadingStrategy get(FileSystem fileSystem, Path input, Path output);
}
