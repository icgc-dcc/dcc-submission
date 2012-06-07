package org.icgc.dcc.service;

import static com.google.common.base.Preconditions.checkArgument;

import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.User;

import com.google.inject.Inject;

public class BaseRelease implements HasRelease {

  protected Release release;

  @Inject
  private DccFileSystem dccFilesystem; // TODO: constructor injection

  public BaseRelease(Release release) {
    checkArgument(this.dccFilesystem != null);
    this.release = release;
  }

  @Override
  public ReleaseFileSystem getReleaseFilesystem(User user) {
    return this.dccFilesystem.getReleaseFilesystem(this.release, user);
  }

  @Override
  public Release getRelease() {
    return release;
  }
}
