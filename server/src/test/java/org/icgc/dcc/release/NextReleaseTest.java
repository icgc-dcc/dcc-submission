package org.icgc.dcc.release;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.DictionaryState;
import org.icgc.dcc.filesystem.DccFileSystem;
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

  private Release mockRelease;

  private Dictionary dictionary;

  private Datastore mockDatastore;

  private Query<Release> mockReleaseQuery;

  private Query<Dictionary> queryDict;

  private UpdateOperations<Release> mockReleaseUpdates;

  private UpdateOperations<Dictionary> updatesDict;

  private DccFileSystem fs;

  private ReleaseService mockReleaseService;

  private DictionaryService mockDictionaryService;

  private static final String NEXT_RELEASE_NAME = "release2";

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    mockRelease = mock(Release.class);
    mockReleaseUpdates = mock(UpdateOperations.class);
    updatesDict = mock(UpdateOperations.class);
    fs = mock(DccFileSystem.class);
    mockReleaseService = mock(ReleaseService.class);
    mockDictionaryService = mock(DictionaryService.class);

    when(mockRelease.getState()).thenReturn(ReleaseState.OPENED);
    List<Submission> submissions = new ArrayList<Submission>();
    Submission s = new Submission();
    s.setState(SubmissionState.SIGNED_OFF);
    submissions.add(s);
    when(mockRelease.getSubmissions()).thenReturn(submissions);
    when(mockRelease.getName()).thenReturn("my_release_name");

    mockDatastore = mock(Datastore.class);

    when(mockDatastore.createUpdateOperations(Release.class)).thenReturn(mockReleaseUpdates);
    when(mockDatastore.createUpdateOperations(Dictionary.class)).thenReturn(updatesDict);

    when(mockReleaseUpdates.disableValidation()).thenReturn(mockReleaseUpdates);
    when(mockReleaseUpdates.set(anyString(), anyString())).thenReturn(mockReleaseUpdates);

    mockReleaseQuery = mock(Query.class);
    queryDict = mock(Query.class);
    when(mockDatastore.createQuery(Release.class)).thenReturn(mockReleaseQuery);
    when(mockDatastore.createQuery(Dictionary.class)).thenReturn(queryDict);

    when(mockReleaseQuery.filter(anyString(), any())).thenReturn(mockReleaseQuery);
    when(queryDict.filter(anyString(), any())).thenReturn(queryDict);

    when(mockReleaseService.getFromName("not_existing_release")).thenReturn(null);
    when(mockDictionaryService.getFromVersion("existing_dictionary")).thenReturn(mock(Dictionary.class));

    nextRelease = new NextRelease(mockRelease, mockDatastore, fs, mockReleaseService, mockDictionaryService);
  }

  @Test(expected = IllegalReleaseStateException.class)
  public void test_NextRelease_throwsWhenBadReleaseState() {
    when(mockRelease.getState()).thenReturn(ReleaseState.COMPLETED);

    new NextRelease(mockRelease, mockDatastore, fs, mockReleaseService, mockDictionaryService);
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

    verify(mockDatastore).update(mockReleaseQuery, mockReleaseUpdates);
  }

  @Test
  public void test_release_setPreviousStateToCompleted() {
    releaseSetUp();

    nextRelease.release(NEXT_RELEASE_NAME);

    verify(mockRelease).setState(ReleaseState.COMPLETED);
  }

  @Test
  public void test_release_setNewStateToOpened() {
    releaseSetUp();

    NextRelease newRelease = nextRelease.release(NEXT_RELEASE_NAME);

    assertTrue(newRelease.getRelease().getState() == ReleaseState.OPENED);
  }

  @Test
  public void test_release_datastoreUpdated() {
    releaseSetUp();

    NextRelease newRelease = nextRelease.release(NEXT_RELEASE_NAME);

    verify(mockDatastore).createUpdateOperations(Release.class);
    verify(mockReleaseUpdates).set("state", ReleaseState.COMPLETED);
    verify(mockReleaseUpdates).set("releaseDate", mockRelease.getReleaseDate());
    verify(mockDatastore).update(mockRelease, mockReleaseUpdates);
    verify(mockDatastore).save(newRelease.getRelease());
  }

  @Test
  public void test_release_correctReturnValue() {
    releaseSetUp();

    NextRelease newRelease = nextRelease.release(NEXT_RELEASE_NAME);

    assertTrue(newRelease.getRelease().getName().equals(NEXT_RELEASE_NAME));
    assertTrue(newRelease.getRelease().getDictionaryVersion().equals("0.6c"));
  }

  @Test
  public void test_release_newDictionarySet() {
    releaseSetUp();

    NextRelease newRelease = nextRelease.release(NEXT_RELEASE_NAME);

    assertTrue(newRelease.getRelease().getDictionaryVersion().equals("0.6c"));
  }

  @Test
  public void test_release_dictionaryClosed() {
    releaseSetUp();

    assertTrue(mockRelease.getDictionaryVersion().equals(dictionary.getVersion()));
    assertTrue(dictionary.getState() == DictionaryState.OPENED);

    nextRelease.release(NEXT_RELEASE_NAME);

    // TODO reinstate this test once NextRelease is rewritten to use services
    // verify(dictionary).close();
  }

  @Test(expected = ReleaseException.class)
  public void test_release_throwsMissingDictionaryException() {
    assertTrue(mockRelease.getDictionaryVersion() == null);

    nextRelease.release("Release2");
  }

  @Ignore
  @Test(expected = ReleaseException.class)
  public void test_release_newReleaseUniqueness() {
    // TODO reinstate once NextRelease is fixed to make mocking easier
    releaseSetUp();

    nextRelease.release(mockRelease.getName());
  }

  @Ignore
  @Test
  public void test_validate() {
    // TODO Create tests once the validation is implemented
  }

  @Test
  public void test_update_valid() {
    Release mockUpdatedRelease = mock(Release.class);
    when(mockUpdatedRelease.getName()).thenReturn("not_existing_release");
    when(mockUpdatedRelease.getDictionaryVersion()).thenReturn("existing_dictionary");

    NextRelease updatedNextRelease = nextRelease.update(mockUpdatedRelease);
    Assert.assertNotNull(updatedNextRelease);
    Assert.assertEquals(mockRelease, updatedNextRelease.getRelease());

    verify(mockRelease).setName("not_existing_release");
    verify(mockRelease).setDictionaryVersion("existing_dictionary");
    verify(mockDatastore).createQuery(Release.class);
    verify(mockReleaseQuery).filter("name", "my_release_name");
    verify(mockDatastore).createUpdateOperations(Release.class);
    verify(mockReleaseUpdates).set("name", "not_existing_release");
    verify(mockReleaseUpdates).set("dictionaryVersion", "existing_dictionary");
    verify(mockDatastore).update(mockReleaseQuery, mockReleaseUpdates);
  }

  @Test(expected = ReleaseException.class)
  public void test_update_invalidName() {
    Release mockUpdatedRelease = mock(Release.class);
    when(mockUpdatedRelease.getName()).thenReturn("existing_release");
    when(mockReleaseService.getFromName("existing_release")).thenReturn(mock(Release.class));
    nextRelease.update(mockUpdatedRelease);
  }

  @Test(expected = ReleaseException.class)
  public void test_update_invalidDictionaryVersion() {
    Release mockUpdatedRelease = mock(Release.class);
    when(mockUpdatedRelease.getName()).thenReturn("unexisting_dictionary");
    when(mockDictionaryService.getFromVersion("unexisting_dictionary")).thenReturn(null);
    nextRelease.update(mockUpdatedRelease);
  }

  private void releaseSetUp() {
    dictionary = mock(Dictionary.class);
    when(dictionary.getState()).thenReturn(DictionaryState.OPENED);
    when(dictionary.getVersion()).thenReturn("0.6c");
    when(mockRelease.getDictionaryVersion()).thenReturn("0.6c");

    when(mockReleaseUpdates.set("state", ReleaseState.COMPLETED)).thenReturn(mockReleaseUpdates);
    when(mockReleaseUpdates.set("releaseDate", mockRelease.getReleaseDate())).thenReturn(mockReleaseUpdates);
  }

  private Submission signOffSetUp() {
    Submission submission = mock(Submission.class);
    when(mockReleaseUpdates.set("submissions.$.state", SubmissionState.SIGNED_OFF)).thenReturn(mockReleaseUpdates);
    return submission;
  }
}
