package org.icgc.dcc.service;

import static org.junit.Assert.assertEquals;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.junit.Test;

public class CompletedReleaseTest {

  @Test(expected = IllegalReleaseStateException.class)
  public void testException() {
    Release release = new Release();
    new CompletedRelease(release);
  }

  @Test
  public void testState() {
    Release release = new Release();
    release.setState(ReleaseState.COMPLETED);
    CompletedRelease completedRelease = new CompletedRelease(release);

    assertEquals(completedRelease.getRelease().getState(), ReleaseState.COMPLETED);
  }
}
