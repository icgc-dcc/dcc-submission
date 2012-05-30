package org.icgc.dcc.service;

import static org.junit.Assert.assertEquals;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;
import org.testng.annotations.Test;

public class NextReleaseTest {

  @Test(groups = { "mongodb" })
  public void test() {
    Release release = new Release();
    Submission submission = new Submission();
    release.getSubmissions().add(submission);
    NextRelease nextRelease = new NextRelease(release);

    assertEquals(nextRelease.getRelease().getState(), ReleaseState.OPENED);

    nextRelease.signOff(submission);

    assertEquals(submission.getState(), SubmissionState.SIGNED_OFF);

    Release newRelease = new Release();
    NextRelease newNextRelease = nextRelease.release(newRelease);

    assertEquals(release.getState(), ReleaseState.COMPLETED);
    assertEquals(newRelease.getState(), ReleaseState.OPENED);
  }
}
