package org.icgc.dcc.service;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.ReleaseState;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.FieldEnd;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;

public class NextReleaseTest {

  private NextRelease nextRelease;

  private Release release;

  private Release release2;

  private Datastore ds;

  private Query<Release> query;

  private FieldEnd<Query<Release>> fieldEnd;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    release = mock(Release.class);
    when(release.getState()).thenReturn(ReleaseState.OPENED);

    ds = mock(Datastore.class);

    nextRelease = new NextRelease(release, ds);

    when(ds.createUpdateOperations(Release.class)).thenReturn(mock(UpdateOperations.class));

    query = mock(Query.class);
    when(ds.createQuery(Release.class)).thenReturn(query);

    fieldEnd = mock(FieldEnd.class);

    when(fieldEnd.equal(anyString())).thenReturn(query);
  }

  @Test
  public void test_NextRelease_throwsWhenBadReleaseState() {
    when(release.getState()).thenReturn(ReleaseState.COMPLETED);

    try {
      new NextRelease(release, ds);
      fail("No error thrown despite incorrect release state");
    } catch(IllegalReleaseStateException e) {
      return;
    }
  }

  @Test
  public void test_signOff_stateSet() {
    Submission submission = signOffSetUp();

    nextRelease.signOff(submission);

    verify(submission).setState(SubmissionState.SIGNED_OFF);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void test_signOff_submissionSaved() {
    Submission submission = signOffSetUp();

    nextRelease.signOff(submission);

    verify(ds).update(eq(query), any(UpdateOperations.class));
  }

  @Test
  public void test_release_setPreviousStateToCompleted() {
    releaseSetUp();

    nextRelease.release(release2);

    verify(release).setState(ReleaseState.COMPLETED);
  }

  @Test
  public void test_release_setNewStateToOpened() {
    releaseSetUp();

    nextRelease.release(release2);

    verify(release2).setState(ReleaseState.OPENED);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void test_release_datastoreUpdated() {
    releaseSetUp();

    nextRelease.release(release2);

    verify(ds).createUpdateOperations(Release.class);
    verify(ds).createQuery(Release.class);
    verify(ds).update(eq(query), any(UpdateOperations.class));
    verify(ds).save(release2);
  }

  @Test
  public void test_release_correctReturnValue() {
    releaseSetUp();

    NextRelease newRelease = nextRelease.release(release2);

    assertTrue(newRelease.getRelease().equals(release2));
  }

  @Ignore
  @Test
  public void test_validate() {
    // TODO Create tests once the validation is implemented
  }

  private void releaseSetUp() {
    release2 = mock(Release.class);
    when(release2.getState()).thenReturn(ReleaseState.OPENED);

    when(query.field("name")).thenReturn(fieldEnd);
  }

  private Submission signOffSetUp() {
    Submission submission = mock(Submission.class);
    when(query.field("submissions.accessionId")).thenReturn(fieldEnd);
    return submission;
  }
}
