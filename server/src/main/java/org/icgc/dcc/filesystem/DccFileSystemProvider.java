package org.icgc.dcc.filesystem;

import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.model.Projects;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;

public class DccFileSystemProvider implements Provider<DccFileSystem> {

  @Inject
  private Config config;

  @Inject
  private Projects projects;

  @Inject
  private FileSystem fileSystem;

  @Override
  public DccFileSystem get() {
    return new DccFileSystem(this.config, this.projects, this.fileSystem);
  }

}
