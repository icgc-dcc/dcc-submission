package org.icgc.dcc.filesystem;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class FilesystemModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Configuration.class).toInstance(new Configuration());
    bind(FileSystem.class).toProvider(FilesystemProvider.class).in(Singleton.class);
    bind(DccFilesystem.class).toProvider(DccFilesystemProvider.class).in(Singleton.class);
  }
}
