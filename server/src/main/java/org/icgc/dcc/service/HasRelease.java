package org.icgc.dcc.service;

import org.icgc.dcc.filesystem.ReleaseFilesystem;
import org.icgc.dcc.model.Release;

public class HasRelease {

  protected Release release;

  public HasRelease(Release release) {
    this.release = release;
  }

  public ReleaseFilesystem getReleaseFilesystem() {
    // TODO implementation have to wait till ReleaseFilesystem is finished
    return null;
  }

  public Release getRelease() {
    return release;
  }
}
