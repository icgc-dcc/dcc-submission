/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.release;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.val;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.core.model.InvalidStateException;
import org.icgc.dcc.submission.dictionary.DictionaryService;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.DictionaryState;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;

public class ReleaseServiceNextReleaseTest {

  private ReleaseService releaseService;

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

  private DictionaryService mockDictionaryService;

  private static final String FIRST_RELEASE_NAME = "release1";
  private static final String NEXT_RELEASE_NAME = "release2";

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    release = mock(Release.class);
    updates = mock(UpdateOperations.class);
    updatesDict = mock(UpdateOperations.class);
    fs = mock(DccFileSystem.class);
    mockReleaseFileSystem = mock(ReleaseFileSystem.class);
    mockDictionaryService = mock(DictionaryService.class);

    when(fs.getReleaseFilesystem(any(Release.class))).thenReturn(mockReleaseFileSystem);

    when(release.getState()).thenReturn(ReleaseState.OPENED);
    List<Submission> submissions = new ArrayList<Submission>();
    Submission s = new Submission();
    s.setState(SubmissionState.SIGNED_OFF);
    submissions.add(s);
    when(release.getSubmissions()).thenReturn(submissions);
    when(release.getName()).thenReturn(FIRST_RELEASE_NAME);
    when(release.getProjectKeys()).thenReturn(Arrays.asList("proj1"));
    when(release.getSubmissions()).thenReturn(submissions);
    when(release.getState()).thenReturn(ReleaseState.OPENED).thenReturn(ReleaseState.COMPLETED);

    ds = mock(Datastore.class);
    mockMorphia = mock(Morphia.class);

    when(ds.createUpdateOperations(Release.class)).thenReturn(updates);
    when(ds.createUpdateOperations(Dictionary.class)).thenReturn(updatesDict);

    when(updates.set(anyString(), anyString())).thenReturn(updates);

    query = mock(Query.class);
    queryDict = mock(Query.class);
    when(ds.createQuery(Release.class)).thenReturn(query);
    when(ds.createQuery(Dictionary.class)).thenReturn(queryDict);

    when(query.filter(anyString(), any())).thenReturn(query);
    when(queryDict.filter(anyString(), any())).thenReturn(queryDict);

    val mockMailService = mock(MailService.class);

    releaseService = spy(new ReleaseService(mockMorphia, ds, fs, mockMailService));

    doReturn(null).when(releaseService).getReleaseByName(anyString());
    doReturn(release).when(releaseService).getNextRelease();

    Dictionary dictionary = mock(Dictionary.class);
    when(mockDictionaryService.getDictionaryByVersion("existing_dictionary")).thenReturn(dictionary);
  }

  @Test
  public void test_release_setPreviousStateToCompleted() throws InvalidStateException {
    releaseSetUp();

    releaseService.release(NEXT_RELEASE_NAME);

    verify(mockReleaseFileSystem)
        .setUpNewReleaseFileSystem(
            anyString(), anyString(),
            any(ReleaseFileSystem.class),
            anyListOf(String.class), anyListOf(String.class));
    verify(release).complete();
  }

  @Test
  public void test_release_setNewStateToOpened() throws InvalidStateException {
    releaseSetUp();

    Release newRelease = releaseService.release(NEXT_RELEASE_NAME);

    assertTrue(newRelease.getState() == ReleaseState.OPENED);
  }

  @Test
  public void test_release_datastoreUpdated() throws InvalidStateException {
    releaseSetUp();

    Release newRelease = releaseService.release(NEXT_RELEASE_NAME);

    verify(ds).save(newRelease);
    verify(ds).createUpdateOperations(Release.class);
    verify(updates).set("state", ReleaseState.COMPLETED);
    verify(updates).set("releaseDate", release.getReleaseDate());
    verify(updates).set("submissions", release.getSubmissions());
  }

  @Test
  public void test_release_correctReturnValue() throws InvalidStateException {
    releaseSetUp();

    Release newRelease = releaseService.release(NEXT_RELEASE_NAME);

    assertTrue(newRelease.getName().equals(NEXT_RELEASE_NAME));
    assertTrue(newRelease.getDictionaryVersion().equals("0.6c"));
  }

  @Test
  public void test_release_newDictionarySet() throws InvalidStateException {
    releaseSetUp();

    Release newRelease = releaseService.release(NEXT_RELEASE_NAME);

    assertTrue(newRelease.getDictionaryVersion().equals("0.6c"));
  }

  @Test
  public void test_release_dictionaryClosed() throws InvalidStateException {
    releaseSetUp();

    assertTrue(release.getDictionaryVersion().equals(dictionary.getVersion()));
    assertTrue(dictionary.getState() == DictionaryState.OPENED);

    releaseService.release(NEXT_RELEASE_NAME);

    // TODO reinstate this test once NextRelease is rewritten to use services
    // verify(dictionary).close();
  }

  @Test(expected = InvalidStateException.class)
  public void test_release_throwsMissingDictionaryException() throws InvalidStateException {
    assertTrue(release.getDictionaryVersion() == null);

    releaseService.release("Release2");
  }

  @Ignore
  @Test(expected = ReleaseException.class)
  public void test_release_newReleaseUniqueness() throws InvalidStateException {
    // TODO reinstate once NextRelease is fixed to make mocking easier
    releaseSetUp();

    releaseService.release(release.getName());
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

  @SuppressWarnings("unused")
  private Submission signOffSetUp() {
    Submission submission = mock(Submission.class);
    when(updates.set("submissions.$.state", SubmissionState.SIGNED_OFF)).thenReturn(updates);
    return submission;
  }

}
