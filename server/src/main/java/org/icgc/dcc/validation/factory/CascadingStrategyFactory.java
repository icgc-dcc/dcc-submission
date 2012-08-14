package org.icgc.dcc.validation.factory;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.validation.CascadingStrategy;

public interface CascadingStrategyFactory {
  public CascadingStrategy get(Path input, Path output);
}
