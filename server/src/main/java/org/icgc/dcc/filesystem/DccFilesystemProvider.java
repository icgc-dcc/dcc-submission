package org.icgc.dcc.filesystem;

import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.model.Projects;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;

public class DccFilesystemProvider implements Provider<DccFilesystem> {

  @Inject
  private Config config;

  @Inject
  private Projects projects;

  @Inject
  private FileSystem fileSystem;

  @Override
  public DccFilesystem get() {
    return new DccFilesystem(this.config, this.projects, this.fileSystem);
  }

}
