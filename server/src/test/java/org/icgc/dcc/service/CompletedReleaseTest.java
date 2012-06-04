package org.icgc.dcc.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.junit.Test;

public class CompletedReleaseTest {

  @Test
  public void testState() {
    Release release = mock(Release.class);
    CompletedRelease completedRelease = mock(CompletedRelease.class);

    release.setState(ReleaseState.COMPLETED);

    verify(release).setState(ReleaseState.COMPLETED);
    when(release.getState()).thenReturn(ReleaseState.COMPLETED);
    when(completedRelease.getRelease()).thenReturn(release);
    when(completedRelease.getRelease().getState()).thenReturn(ReleaseState.COMPLETED);

  }

}
