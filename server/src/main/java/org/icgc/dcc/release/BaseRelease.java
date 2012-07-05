package org.icgc.dcc.release;

import org.icgc.dcc.core.model.User;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.release.model.Release;

import com.google.inject.Inject;

public class BaseRelease implements HasRelease {

  protected Release release;

  @Inject
  private DccFileSystem dccFilesystem; // TODO: constructor injection

  public BaseRelease(Release release) {
    // checkArgument(this.dccFilesystem != null); // TODO: uncomment once constructor injection is set up
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
