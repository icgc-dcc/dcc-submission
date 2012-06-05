package org.icgc.dcc.filesystem;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;

public class DccFilesystemProvider implements Provider<DccFilesystem> {

  @Inject
  private Config config;

  @Inject
  private IFilesystem filesystem;

  @Override
  public DccFilesystem get() {
    return new DccFilesystem(this.config, this.filesystem);
  }

}
