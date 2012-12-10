package org.icgc.dcc.release;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.DictionaryState;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;

public class NextReleaseTest {

  private NextRelease nextRelease;

  private Release release;

  private Dictionary dictionary;

  private Datastore ds;

  private Morphia mockMorphia;

  private Query<Release> query;

  private Query<Dictionary> queryDict;

  private UpdateOperations<Release> updates;

  private UpdateOperations<Dictionary> updatesDict;

  private DccFileSystem fs;

  private ReleaseFileSystem mockReleaseFileSystem;

  private ReleaseService mockReleaseService;

  private DictionaryService mockDictionaryService;

  private static final String NEXT_RELEASE_NAME = "release2";

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    release = mock(Release.class);
    updates = mock(UpdateOperations.class);
    updatesDict = mock(UpdateOperations.class);
    fs = mock(DccFileSystem.class);
    mockReleaseFileSystem = mock(ReleaseFileSystem.class);
    mockReleaseService = mock(ReleaseService.class);
    mockDictionaryService = mock(DictionaryService.class);

    when(fs.getReleaseFilesystem(any(Release.class))).thenReturn(mockReleaseFileSystem);

    when(release.getState()).thenReturn(ReleaseState.OPENED);
    List<Submission> submissions = new ArrayList<Submission>();
    Submission s = new Submission();
    s.setState(SubmissionState.SIGNED_OFF);
    submissions.add(s);
    when(release.getSubmissions()).thenReturn(submissions);
    when(release.getName()).thenReturn("my_release_name");
    when(release.getProjectKeys()).thenReturn(Arrays.asList("proj1"));
    when(release.getSubmissions()).thenReturn(submissions);

    ds = mock(Datastore.class);
    mockMorphia = mock(Morphia.class);

    when(ds.createUpdateOperations(Release.class)).thenReturn(updates);
    when(ds.createUpdateOperations(Dictionary.class)).thenReturn(updatesDict);

    when(updates.disableValidation()).thenReturn(updates);
    when(updates.set(anyString(), anyString())).thenReturn(updates);

    query = mock(Query.class);
    queryDict = mock(Query.class);
    when(ds.createQuery(Release.class)).thenReturn(query);
    when(ds.createQuery(Dictionary.class)).thenReturn(queryDict);

    when(query.filter(anyString(), any())).thenReturn(query);
    when(queryDict.filter(anyString(), any())).thenReturn(queryDict);

    when(mockReleaseService.getFromName("not_existing_release")).thenReturn(null);
    Dictionary dictionary = mock(Dictionary.class);
    when(mockDictionaryService.getFromVersion("existing_dictionary")).thenReturn(dictionary);

    nextRelease = new NextRelease(null, release, mockMorphia, ds, fs); // FIXME
  }

  @Test(expected = IllegalReleaseStateException.class)
  public void test_NextRelease_throwsWhenBadReleaseState() {
    when(release.getState()).thenReturn(ReleaseState.COMPLETED);

    new NextRelease(null, release, mockMorphia, ds, fs); // FIXME
  }

  @Ignore
  @Test
  public void test_signOff_stateSet() {
    Submission submission = signOffSetUp();
    // nextRelease.signOff(submission);
    verify(submission).setState(SubmissionState.SIGNED_OFF);
  }

  @Ignore
  @Test
  public void test_signOff_submissionSaved() {
    Submission submission = signOffSetUp();
    // nextRelease.signOff(submission);
    verify(ds).update(query, updates);
  }

  @Test
  public void test_release_setPreviousStateToCompleted() {
    releaseSetUp();

    nextRelease.release(NEXT_RELEASE_NAME);

    verify(release).setState(ReleaseState.COMPLETED);
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

    verify(ds).createUpdateOperations(Release.class);
    verify(updates).set("state", ReleaseState.COMPLETED);
    verify(updates).set("releaseDate", release.getReleaseDate());
    verify(ds).update(release, updates);
    verify(ds).save(newRelease.getRelease());
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

    assertTrue(release.getDictionaryVersion().equals(dictionary.getVersion()));
    assertTrue(dictionary.getState() == DictionaryState.OPENED);

    nextRelease.release(NEXT_RELEASE_NAME);

    // TODO reinstate this test once NextRelease is rewritten to use services
    // verify(dictionary).close();
  }

  @Test(expected = ReleaseException.class)
  public void test_release_throwsMissingDictionaryException() {
    assertTrue(release.getDictionaryVersion() == null);

    nextRelease.release("Release2");
  }

  @Ignore
  @Test(expected = ReleaseException.class)
  public void test_release_newReleaseUniqueness() {
    // TODO reinstate once NextRelease is fixed to make mocking easier
    releaseSetUp();

    nextRelease.release(release.getName());
  }

  @Ignore
  @Test
  public void test_validate() {
    // TODO Create tests once the validation is implemented
  }

  private void releaseSetUp() {
    dictionary = mock(Dictionary.class);
    when(dictionary.getState()).thenReturn(DictionaryState.OPENED);
    when(dictionary.getVersion()).thenReturn("0.6c");
    when(release.getDictionaryVersion()).thenReturn("0.6c");

    when(updates.set("state", ReleaseState.COMPLETED)).thenReturn(updates);
    when(updates.set("releaseDate", release.getReleaseDate())).thenReturn(updates);
  }

  private Submission signOffSetUp() {
    Submission submission = mock(Submission.class);
    when(updates.set("submissions.$.state", SubmissionState.SIGNED_OFF)).thenReturn(updates);
    return submission;
  }
}
