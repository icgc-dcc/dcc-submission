package org.icgc.dcc.release;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.DictionaryState;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;

public class NextReleaseTest {

  private NextRelease nextRelease;

  private Release release;

  private Release release2;

  private Dictionary dictionary;

  private Datastore ds;

  private Query<Release> query;

  private UpdateOperations<Release> updates;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    release = mock(Release.class);
    updates = mock(UpdateOperations.class);
    when(release.getState()).thenReturn(ReleaseState.OPENED);

    ds = mock(Datastore.class);

    nextRelease = new NextRelease(release, ds);

    when(ds.createUpdateOperations(Release.class)).thenReturn(updates);
    when(updates.disableValidation()).thenReturn(updates);

    query = mock(Query.class);
    when(ds.createQuery(Release.class)).thenReturn(query);
    when(query.filter(anyString(), any())).thenReturn(query);
  }

  @Test(expected = IllegalReleaseStateException.class)
  public void test_NextRelease_throwsWhenBadReleaseState() {
    when(release.getState()).thenReturn(ReleaseState.COMPLETED);

    new NextRelease(release, ds);
  }

  @Test
  public void test_signOff_stateSet() {
    Submission submission = signOffSetUp();

    nextRelease.signOff(submission);

    verify(submission).setState(SubmissionState.SIGNED_OFF);
  }

  @Test
  public void test_signOff_submissionSaved() {
    Submission submission = signOffSetUp();

    nextRelease.signOff(submission);

    verify(ds).update(query, updates);
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

  @Test
  public void test_release_datastoreUpdated() {
    releaseSetUp();

    nextRelease.release(release2);

    verify(ds).createUpdateOperations(Release.class);
    verify(updates).set("state", ReleaseState.COMPLETED);
    verify(updates).set("releaseDate", release.getReleaseDate());
    verify(ds).update(release, updates);
    verify(ds).save(release2);
  }

  @Test
  public void test_release_correctReturnValue() {
    releaseSetUp();

    NextRelease newRelease = nextRelease.release(release2);

    assertTrue(newRelease.getRelease().equals(release2));
  }

  @Test
  public void test_release_newDictionarySet() {
    releaseSetUp();

    assertTrue(release2.getDictionary() == null);

    nextRelease.release(release2);

    verify(release2).setDictionary(dictionary);
  }

  @Test
  public void test_release_dictionaryClosed() {
    releaseSetUp();

    assertTrue(release.getDictionary().equals(dictionary));
    assertTrue(dictionary.getState() == DictionaryState.OPENED);

    nextRelease.release(release2);

    verify(dictionary).close();
  }

  @Test(expected = ReleaseException.class)
  public void test_release_throwsMissingDictionaryException() {
    assertTrue(release.getDictionary() == null);

    nextRelease.release(release);
  }

  @Test(expected = ReleaseException.class)
  public void test_release_newReleaseUniqueness() {
    releaseSetUp();

    nextRelease.release(release);
  }

  @Ignore
  @Test
  public void test_validate() {
    // TODO Create tests once the validation is implemented
  }

  private void releaseSetUp() {
    dictionary = mock(Dictionary.class);
    when(release.getDictionary()).thenReturn(dictionary);
    when(dictionary.getState()).thenReturn(DictionaryState.OPENED);

    release2 = mock(Release.class);
    when(release2.getState()).thenReturn(ReleaseState.OPENED);
    when(release2.getDictionary()).thenReturn(null);
    when(updates.set("state", ReleaseState.COMPLETED)).thenReturn(updates);
    when(updates.set("releaseDate", release.getReleaseDate())).thenReturn(updates);
  }

  private Submission signOffSetUp() {
    Submission submission = mock(Submission.class);
    when(updates.set("submissions.$.state", SubmissionState.SIGNED_OFF)).thenReturn(updates);
    return submission;
  }
}
