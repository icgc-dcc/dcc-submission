package org.icgc.dcc.release;

import org.icgc.dcc.core.model.User;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.release.model.Release;

public interface HasRelease {

  public ReleaseFileSystem getReleaseFilesystem(User user);

  public Release getRelease();
}
