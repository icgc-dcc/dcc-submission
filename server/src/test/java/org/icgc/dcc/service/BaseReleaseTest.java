package org.icgc.dcc.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.icgc.dcc.model.Release;
import org.junit.Test;

public class BaseReleaseTest {

  @Test
  public void test() {
    Release release = mock(Release.class);

    BaseRelease baseRelease = new BaseRelease(release);

    assertEquals(baseRelease.getRelease(), release);
  }

}
