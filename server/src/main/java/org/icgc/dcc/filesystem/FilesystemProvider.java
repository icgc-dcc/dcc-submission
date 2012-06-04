package org.icgc.dcc.filesystem;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;

public class FilesystemProvider implements Provider<IFilesystem> {

  @Inject
  private Config config;

  @Override
  public IFilesystem get() {
    return new LocalFilesystem(this.config);
  }
}
