package org.icgc.dcc.release;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.icgc.dcc.core.model.BaseEntity;
import org.icgc.dcc.core.model.DccModelOptimisticLockException;
import org.icgc.dcc.core.model.InvalidStateException;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.typesafe.config.Config;

public class ReleaseServiceTest {

  private DccLocking dccLocking;

  private Datastore datastore;

  private Dictionary dictionary;

  private DictionaryService dictionaryService;

  private ReleaseService releaseService;

  private Release release;

  private DccFileSystem fs;

  private Config config;

  private DccFileSystem mockDccFileSystem;

  private ReleaseFileSystem mockReleaseFileSystem;

  final private String testDbName = "testDb";

  @Before
  public void setUp() {
    try {

      // use local host as test MongoDB for now
      Mongo mongo = new Mongo("localhost");
      Morphia morphia = new Morphia();
      morphia.map(BaseEntity.class);
      datastore = morphia.createDatastore(mongo, testDbName);
      dccLocking = mock(DccLocking.class);
      fs = mock(DccFileSystem.class);
      mockReleaseFileSystem = mock(ReleaseFileSystem.class);
      mockDccFileSystem = mock(DccFileSystem.class);

      config = mock(Config.class);

      when(fs.getReleaseFilesystem(any(Release.class))).thenReturn(mockReleaseFileSystem);

      // Clear out the test database before each test
      datastore.delete(datastore.createQuery(Dictionary.class));
      datastore.delete(datastore.createQuery(Release.class));
      datastore.delete(datastore.createQuery(Project.class));

      // Set up a minimal test case
      dictionary = new Dictionary();
      dictionary.setVersion("foo");

      release = new Release("release1");

      Project project1 = new Project("Project One", "p1");
      Project project2 = new Project("Project Two", "p2");
      Project project3 = new Project("Project Three", "p3");

      Submission validSubmission = new Submission();
      validSubmission.setState(SubmissionState.VALID);
      validSubmission.setProjectKey(project1.getKey());

      Submission notValidatedSubmission = new Submission();
      notValidatedSubmission.setState(SubmissionState.NOT_VALIDATED);
      notValidatedSubmission.setProjectKey(project2.getKey());

      Submission queuedSubmission = new Submission();
      queuedSubmission.setState(SubmissionState.QUEUED);
      queuedSubmission.setProjectKey(project3.getKey());

      release.addSubmission(validSubmission);
      release.addSubmission(notValidatedSubmission);
      release.addSubmission(queuedSubmission);

      release.setDictionaryVersion(dictionary.getVersion());

      // Create the releaseService and populate it with the initial release
      releaseService = new ReleaseService(dccLocking, morphia, datastore, fs, config);
      dictionaryService = new DictionaryService(morphia, datastore, mockDccFileSystem, releaseService);
      dictionaryService.add(dictionary);
      releaseService.createInitialRelease(release);
    } catch(UnknownHostException e) {
      e.printStackTrace();

      fail(e.getMessage());
    } catch(MongoException e) {
      e.printStackTrace();

      fail(e.getMessage());
    } catch(NullPointerException e) {
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
    assertEquals(release.getId(), releaseService.getNextRelease().getRelease().getId());
    Release newRelease = addNewRelease("release2");
    assertEquals(newRelease.getName(), releaseService.getNextRelease().getRelease().getName());
  }

  @Test
  public void test_getFromName_exists() {
    Assert.assertNotNull(releaseService.getFromName("release1"));
  }

  @Test
  public void test_getFromName_notExists() {
    Assert.assertNull(releaseService.getFromName("dummy"));
  }

  // @Test; The workflow seems to be that a Release has to be created first and then projects are added to it. This test
  // only works if projects can be included with the createInitialRelease call, which they can't.
  public void test_createInitialRelease_isPersistedToFS() {
    Set<String> projectKeys = new HashSet<String>();
    projectKeys.add("p1");
    projectKeys.add("p2");
    projectKeys.add("p3");
    verify(this.fs).createReleaseFilesystem(release, projectKeys);
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
    assertEquals(1, releaseService.list().size());
    addNewRelease("release2");
    assertEquals(2, releaseService.list().size());
  }

  // @Test
  public void test_can_release() throws InvalidStateException, DccModelOptimisticLockException {
    NextRelease nextRelease = releaseService.getNextRelease();
    Release nextReleaseRelease = nextRelease.getRelease();
    assertTrue(!nextRelease.atLeastOneSignedOff(nextReleaseRelease));

    List<String> projectKeys = new ArrayList<String>();
    projectKeys.add("p1");
    String user = "admin";
    releaseService.signOff(nextReleaseRelease, projectKeys, user);

    nextRelease = releaseService.getNextRelease();
    assertTrue(nextRelease.atLeastOneSignedOff(nextReleaseRelease));
  }

  // @Test
  public void test_update_valid() {
    Release mockUpdatedRelease = mock(Release.class);
    when(mockUpdatedRelease.getName()).thenReturn("not_existing_release");
    when(mockUpdatedRelease.getDictionaryVersion()).thenReturn("existing_dictionary");

    Release updatedRelease = releaseService.update(mockUpdatedRelease);
    Assert.assertNotNull(updatedRelease);
    Assert.assertEquals("not_existing_release", updatedRelease.getName());
    Assert.assertEquals("existing_dictionary", updatedRelease.getDictionaryVersion());
  }

  private Release addNewRelease(String name) {
    Release newRelease = new Release(name);

    List<String> projectKeys = new ArrayList<String>();
    projectKeys.add("p1");
    String user = "admin";
    try {
      releaseService.signOff(newRelease, projectKeys, user);
    } catch(InvalidStateException e) {
      throw new RuntimeException(e);
    } catch(DccModelOptimisticLockException e) {
      throw new RuntimeException(e);
    }

    NextRelease nextRelease = null;
    try {
      nextRelease = releaseService.getNextRelease().release(newRelease.getName());
    } catch(InvalidStateException e) {
      e.printStackTrace();
    }
    return nextRelease.getRelease();
  }
}
