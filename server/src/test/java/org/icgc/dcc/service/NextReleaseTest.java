package org.icgc.dcc.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;
import org.testng.annotations.Test;

public class NextReleaseTest {

  @Test(groups = { "mongodb" })
  public void test() {
    NextRelease nextRelease = mock(NextRelease.class);
    Release release = mock(Release.class);
    Submission submission = mock(Submission.class);

    release.getSubmissions().add(submission);

    when(release.getSubmissions().size()).thenReturn(1);
    when(release.getSubmissions().get(0)).thenReturn(submission);

    nextRelease.signOff(submission);

    when(submission.getState()).thenReturn(SubmissionState.SIGNED_OFF);

    Release newRelease = mock(Release.class);
    NextRelease newNextRelease = nextRelease.release(newRelease);

    when(nextRelease.getRelease().getState()).thenReturn(ReleaseState.COMPLETED);
    when(newNextRelease.getRelease().getState()).thenReturn(ReleaseState.OPENED);
  }
}
