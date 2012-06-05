package org.icgc.dcc.filesystem;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class FilesystemModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(IFilesystem.class).toProvider(FilesystemProvider.class);
    bind(DccFilesystem.class).toProvider(DccFilesystemProvider.class).in(Singleton.class);
  }
}
