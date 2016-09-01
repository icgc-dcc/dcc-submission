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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.icgc.dcc.submission.core.model.DccModelOptimisticLockException;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.SubmissionFileSystem;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.server.core.InvalidStateException;
import org.icgc.dcc.submission.server.repository.CodeListRepository;
import org.icgc.dcc.submission.server.repository.DictionaryRepository;
import org.icgc.dcc.submission.server.repository.ProjectRepository;
import org.icgc.dcc.submission.server.repository.ReleaseRepository;
import org.icgc.dcc.submission.server.service.DictionaryService;
import org.icgc.dcc.submission.server.service.MailService;
import org.icgc.dcc.submission.server.service.ReleaseService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.google.common.base.Throwables;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

import org.junit.Assert;
import lombok.val;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseServiceTest {

  /**
   * Class under test.
   */
  private ReleaseService releaseService;

  /**
   * Dependencies
   */
  private Datastore datastore;
  private Dictionary dictionary;
  private DictionaryService dictionaryService;
  private Release release;

  @Mock
  private SubmissionFileSystem submissionFileSystem;
  @Mock
  private MailService mailService;

  private final static String TEST_DB_NAME = "dcc-test";

  @Before
  public void setUp() {
    try {
      // use local host as test MongoDB for now
      Mongo mongo = new MongoClient("localhost");
      Morphia morphia = new Morphia();
      datastore = morphia.createDatastore(mongo, TEST_DB_NAME);

      // Clear out the test database before each test
      datastore.delete(datastore.createQuery(Dictionary.class));
      datastore.delete(datastore.createQuery(Release.class));
      datastore.delete(datastore.createQuery(Project.class));

      // Set up a minimal test case
      dictionary = new Dictionary();
      dictionary.setVersion("foo");

      val project1 = new Project("Project One", "p1");
      val validSubmission = new Submission();
      validSubmission.setState(SubmissionState.VALID);
      validSubmission.setProjectKey(project1.getKey());

      val project2 = new Project("Project Two", "p2");
      val notValidatedSubmission = new Submission();
      notValidatedSubmission.setState(SubmissionState.NOT_VALIDATED);
      notValidatedSubmission.setProjectKey(project2.getKey());

      val project3 = new Project("Project Three", "p3");
      val queuedSubmission = new Submission();
      queuedSubmission.setState(SubmissionState.QUEUED);
      queuedSubmission.setProjectKey(project3.getKey());

      release = new Release("release1");
      release.addSubmission(validSubmission);
      release.addSubmission(notValidatedSubmission);
      release.addSubmission(queuedSubmission);
      release.setDictionaryVersion(dictionary.getVersion());

      // Create the releaseService and populate it with the initial release
      val releaseRepository = spy(new ReleaseRepository(morphia, datastore));
      val dictionaryRepository = spy(new DictionaryRepository(morphia, datastore));
      val codeListRepository = spy(new CodeListRepository(morphia, datastore));
      val projectRepository = spy(new ProjectRepository(morphia, datastore));

      releaseService = new ReleaseService(mailService, submissionFileSystem,
          releaseRepository, dictionaryRepository, projectRepository);

      dictionaryService = new DictionaryService(releaseService, dictionaryRepository, codeListRepository);
      dictionaryService.addDictionary(dictionary);
      releaseService.createInitialRelease(release);
    } catch (UnknownHostException e) {
      e.printStackTrace();

      fail(e.getMessage());
    } catch (MongoException e) {
      e.printStackTrace();

      fail(e.getMessage());
    } catch (NullPointerException e) {
      e.printStackTrace();

      fail(e.getMessage());
    }
  }

  @After
  public void tearDown() {
    datastore.delete(dictionary);
  }

  // @Test; cannot test release() anymore since we can't mock this: new MorphiaQuery<Project>(morphia, datastore,
  // QProject.project); TODO: find a solution
  public void test_getNextRelease_isCorrectRelease() {
    assertEquals(release.getId(), releaseService.getNextRelease().getId());
    Release newRelease = addNewRelease("release2");
    assertEquals(newRelease.getName(), releaseService.getNextRelease().getName());
  }

  @Test
  public void test_getFromName_exists() {
    Assert.assertNotNull(releaseService.getReleaseByName("release1"));
  }

  @Test
  public void test_getFromName_notExists() {
    Assert.assertNull(releaseService.getReleaseByName("dummy"));
  }

  // @Test; The workflow seems to be that a Release has to be created first and then projects are added to it. This test
  // only works if projects can be included with the createInitialRelease call, which they can't.
  public void test_createInitialRelease_isPersistedToFS() {
    Set<String> projectKeys = new HashSet<String>();
    projectKeys.add("p1");
    projectKeys.add("p2");
    projectKeys.add("p3");
    verify(this.submissionFileSystem).createInitialReleaseFilesystem(release, projectKeys);
  }

  // @Test; cannot test release() anymore since we can't mock this: new MorphiaQuery<Project>(morphia, datastore,
  // QProject.project); TODO: find a solution
  public void test_getCompletedReleases_isCorrectSize() {
    assertEquals(0, releaseService.getCompletedReleases().size());
    addNewRelease("release2");
    assertEquals(1, releaseService.getCompletedReleases().size());
  }

  // @Test; cannot test release() anymore since we can't mock this: new MorphiaQuery<Project>(morphia, datastore,
  // QProject.project); TODO: find a solution
  public void test_list_isCorrectSize() {
    assertEquals(1, releaseService.getReleases().size());
    addNewRelease("release2");
    assertEquals(2, releaseService.getReleases().size());
  }

  // @Test
  public void test_can_release() throws InvalidStateException, DccModelOptimisticLockException {
    Release nextReleaseRelease = releaseService.getNextRelease();
    assertTrue(!nextReleaseRelease.isSignOffAllowed());

    List<String> projectKeys = new ArrayList<String>();
    projectKeys.add("p1");
    String user = "admin";
    releaseService.signOffRelease(projectKeys, user);

    assertTrue(nextReleaseRelease.isSignOffAllowed());
  }

  // @Test
  public void test_update_valid() {
    val updatedRelease = releaseService.updateRelease("not_existing_release", "existing_dictionary");

    assertNotNull(updatedRelease);
    assertEquals("not_existing_release", updatedRelease.getName());
    assertEquals("existing_dictionary", updatedRelease.getDictionaryVersion());
  }

  private Release addNewRelease(String name) {
    val newRelease = new Release(name);

    List<String> projectKeys = new ArrayList<String>();
    projectKeys.add("p1");
    String user = "admin";
    try {
      releaseService.signOffRelease(projectKeys, user);
    } catch (InvalidStateException e) {
      throw new RuntimeException(e);
    } catch (DccModelOptimisticLockException e) {
      throw new RuntimeException(e);
    }

    Release nextRelease = null;
    try {
      nextRelease = releaseService.performRelease(newRelease.getName());
    } catch (InvalidStateException e) {
      Throwables.propagate(e);
    }

    return nextRelease;
  }

}
