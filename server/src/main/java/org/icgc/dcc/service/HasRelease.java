package org.icgc.dcc.service;

import org.icgc.dcc.filesystem.ReleaseFilesystem;
import org.icgc.dcc.model.Release;

public interface HasRelease {

  public ReleaseFilesystem getReleaseFilesystem();

  public Release getRelease();
}
