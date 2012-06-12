package org.icgc.dcc.service;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.junit.Test;

public class CompletedReleaseTest {

  @Test
  public void test_CompletedRelease_throwsIfIllegalState() {
    Release mockRelease = mock(Release.class);
    when(mockRelease.getState()).thenReturn(ReleaseState.OPENED);

    try {
      new CompletedRelease(mockRelease);
      fail("Exception expected but none thrown");
    } catch(IllegalReleaseStateException e) {

    }
  }

  @Test
  public void test_CompletedRelease_doesNotThrow() {
    Release mockRelease = mock(Release.class);
    when(mockRelease.getState()).thenReturn(ReleaseState.COMPLETED);

    new CompletedRelease(mockRelease);
  }

}
