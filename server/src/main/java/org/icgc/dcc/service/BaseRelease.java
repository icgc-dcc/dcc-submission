package org.icgc.dcc.service;

import org.icgc.dcc.filesystem.ReleaseFilesystem;
import org.icgc.dcc.model.Release;

public class BaseRelease implements HasRelease {

  protected Release release;

  public BaseRelease(Release release) {
    this.release = release;
  }

  @Override
  public ReleaseFilesystem getReleaseFilesystem() {
    // TODO implementation have to wait till HDFS Filesystem is finished
    return null;
  }

  @Override
  public Release getRelease() {
    return release;
  }
}
