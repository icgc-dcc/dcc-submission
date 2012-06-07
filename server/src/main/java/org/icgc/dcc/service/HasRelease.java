package org.icgc.dcc.service;

import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.User;

public interface HasRelease {

  public ReleaseFileSystem getReleaseFilesystem(User user);

  public Release getRelease();
}
