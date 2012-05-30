package org.icgc.dcc.service;

import org.icgc.dcc.filesystem.ReleaseFilesystem;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;

public class HasRelease {

  protected Release release;

  public HasRelease(Release release) {
    this.release = release;
    // set release state to be opened when new a release
    this.release.setState(ReleaseState.OPENED);
  }

  public ReleaseFilesystem getReleaseFilesystem() {
    // TODO implementation have to wait till HDFS Filesystem is finished
    return null;
  }

  public Release getRelease() {
    return release;
  }
}
