package org.icgc.dcc.release;

import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;

public class CompletedRelease extends BaseRelease {

  CompletedRelease(Release release, Morphia morphia, Datastore datastore, DccFileSystem fs)
      throws IllegalReleaseStateException {
    super(release, morphia, datastore, fs);
    if(release.getState() != ReleaseState.COMPLETED) {
      throw new IllegalReleaseStateException(release, ReleaseState.COMPLETED);
    }
  }

}
