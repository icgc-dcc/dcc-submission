package org.icgc.dcc.filesystem;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class FileSystemModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Configuration.class).toInstance(new Configuration());
    bind(FileSystem.class).toProvider(FileSystemProvider.class).in(Singleton.class);
    bind(DccFileSystem.class).in(Singleton.class);
  }
}
