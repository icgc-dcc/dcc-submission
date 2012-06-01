package org.icgc.dcc.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.testng.annotations.Test;

public class HasReleaseTest {

  @Test(groups = { "unit" })
  public void test() {
    Release release = mock(Release.class);
    HasRelease hasRelease = mock(HasRelease.class);

    when(hasRelease.getRelease()).thenReturn(release);

    release.setState(ReleaseState.OPENED);

    when(hasRelease.getRelease().getState()).thenReturn(ReleaseState.OPENED);

    release.setName("release");

    when(hasRelease.getRelease().getName()).thenReturn("release");
  }

}
