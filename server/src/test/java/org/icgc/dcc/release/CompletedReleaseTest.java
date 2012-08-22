package org.icgc.dcc.release;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;

public class CompletedReleaseTest {

  @Test
  public void test_CompletedRelease_throwsIfIllegalState() {
    Release mockRelease = mock(Release.class);
    when(mockRelease.getState()).thenReturn(ReleaseState.OPENED);

    try {
      new CompletedRelease(mockRelease, mock(Morphia.class), mock(Datastore.class), mock(DccFileSystem.class));
      fail("Exception expected but none thrown");
    } catch(IllegalReleaseStateException e) {

    }
  }

  @Test
  public void test_CompletedRelease_doesNotThrow() {
    Release mockRelease = mock(Release.class);
    when(mockRelease.getState()).thenReturn(ReleaseState.COMPLETED);

    new CompletedRelease(mockRelease, mock(Morphia.class), mock(Datastore.class), mock(DccFileSystem.class));
  }

}
