package org.icgc.dcc.service;

import static org.junit.Assert.assertEquals;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.junit.Test;

public class HasReleaseTest {

  @Test
  public void test() {
    Release release = new Release();
    HasRelease hasRelease = new HasRelease(release);

    assertEquals(hasRelease.getRelease().getState(), ReleaseState.OPENED);
  }

}
