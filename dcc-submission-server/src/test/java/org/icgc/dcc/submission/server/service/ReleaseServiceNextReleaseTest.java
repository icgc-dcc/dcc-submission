/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.server.service;

import static java.util.Collections.singletonList;
import static org.icgc.dcc.submission.release.model.SubmissionState.SIGNED_OFF;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.DictionaryState;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionFileSystem;
import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.server.core.InvalidStateException;
import org.icgc.dcc.submission.server.repository.CodeListRepository;
import org.icgc.dcc.submission.server.repository.DictionaryRepository;
import org.icgc.dcc.submission.server.repository.ProjectRepository;
import org.icgc.dcc.submission.server.repository.ReleaseRepository;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseServiceNextReleaseTest {

  /**
   * Class under test.
   */
  @InjectMocks
  ReleaseService releaseService;

  /**
   * Dependencies.
   */
  @Mock
  Release release;
  @Mock
  Dictionary dictionary;

  @Mock
  SubmissionFileSystem submissionFileSystem;
  @Mock
  ReleaseFileSystem releaseFileSystem;
  @Mock
  MailService mailService;
  @Mock
  SubmissionService submissionService;

  @Mock
  DictionaryService dictionaryService;

  @Mock
  ReleaseRepository releaseRepository;
  @Mock
  DictionaryRepository dictionaryRepository;
  @Mock
  ProjectRepository projectRepository;
  @Mock
  CodeListRepository codelistRepository;

  static final String FIRST_RELEASE_NAME = "release1";
  static final String NEXT_RELEASE_NAME = "release2";
  static final String PROJECT_NAME = "project1";

  @Before
  public void setUp() throws IOException {
    when(submissionFileSystem.getFileSystem()).thenReturn(FileSystem.getLocal(new Configuration()));
    when(submissionFileSystem.getReleaseFilesystem(any(Release.class), any())).thenReturn(releaseFileSystem);
    when(submissionFileSystem.buildProjectStringPath(anyString(), anyString())).thenReturn("/");

    when(release.getName()).thenReturn(FIRST_RELEASE_NAME);
    when(release.getState()).thenReturn(ReleaseState.OPENED);
    when(release.getState()).thenReturn(ReleaseState.OPENED).thenReturn(ReleaseState.COMPLETED);
    when(submissionService.findSubmissionStateByReleaseName(FIRST_RELEASE_NAME))
        .thenReturn(singletonList(new Submission(PROJECT_NAME, PROJECT_NAME, FIRST_RELEASE_NAME, SIGNED_OFF)));

    when(releaseRepository.findNextRelease()).thenReturn(release);
    when(releaseRepository.findReleaseByName(FIRST_RELEASE_NAME)).thenReturn(release);
    when(releaseRepository.findReleaseByName(NEXT_RELEASE_NAME)).thenReturn(null);

    when(dictionaryRepository.findDictionaryByVersion("0.6c")).thenReturn(dictionary);

    when(dictionaryService.getDictionaryByVersion("existing_dictionary")).thenReturn(dictionary);
  }

  @Test
  public void test_release_setPreviousStateToCompleted() throws InvalidStateException {
    releaseSetUp();

    releaseService.performRelease(NEXT_RELEASE_NAME);

    verify(releaseFileSystem)
        .setUpNewReleaseFileSystem(
            anyString(),
            any(ReleaseFileSystem.class),
            anyListOf(String.class));
    verify(release).complete();
  }

  @Test
  public void test_release_setNewStateToOpened() throws InvalidStateException {
    releaseSetUp();

    Release newRelease = releaseService.performRelease(NEXT_RELEASE_NAME);

    assertTrue(newRelease.getState() == ReleaseState.OPENED);
  }

  @Test
  public void test_release_datastoreUpdated() throws InvalidStateException {
    releaseSetUp();

    Release newRelease = releaseService.performRelease(NEXT_RELEASE_NAME);

    verify(releaseRepository).saveNewRelease(newRelease);
  }

  @Test
  public void test_release_correctReturnValue() throws InvalidStateException {
    releaseSetUp();

    Release newRelease = releaseService.performRelease(NEXT_RELEASE_NAME);

    assertTrue(newRelease.getName().equals(NEXT_RELEASE_NAME));
    assertTrue(newRelease.getDictionaryVersion().equals("0.6c"));
  }

  @Test
  public void test_release_newDictionarySet() throws InvalidStateException {
    releaseSetUp();

    Release newRelease = releaseService.performRelease(NEXT_RELEASE_NAME);

    assertTrue(newRelease.getDictionaryVersion().equals("0.6c"));
  }

  @Test
  public void test_release_dictionaryClosed() throws InvalidStateException {
    releaseSetUp();

    assertTrue(release.getDictionaryVersion().equals(dictionary.getVersion()));
    assertTrue(dictionary.getState() == DictionaryState.OPENED);

    releaseService.performRelease(NEXT_RELEASE_NAME);

    // verify(dictionary).close();
  }

  @Test(expected = InvalidStateException.class)
  public void test_release_throwsMissingDictionaryException() throws InvalidStateException {
    assertTrue(release.getDictionaryVersion() == null);

    releaseService.performRelease("Release2");
  }

  @Ignore
  @Test(expected = ReleaseException.class)
  public void test_release_newReleaseUniqueness() throws InvalidStateException {
    // TODO reinstate once NextRelease is fixed to make mocking easier
    releaseSetUp();

    releaseService.performRelease(release.getName());
  }

  @Ignore
  @Test
  public void test_validate() {
    // TODO Create tests once the validation is implemented
  }

  private void releaseSetUp() {
    when(dictionary.getState()).thenReturn(DictionaryState.OPENED);
    when(dictionary.getVersion()).thenReturn("0.6c");
    when(dictionary.getFileSchemaByFileName(anyString())).thenReturn(Optional.<FileSchema> absent());
    when(release.getDictionaryVersion()).thenReturn("0.6c");
    when(release.isQueued()).thenReturn(false);
  }

  @SuppressWarnings("unused")
  private Submission signOffSetUp() {
    Submission submission = mock(Submission.class);
    return submission;
  }

}
