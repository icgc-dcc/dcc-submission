package org.icgc.dcc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;

import org.icgc.dcc.model.BaseEntity;
import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;
import org.icgc.dcc.model.dictionary.Dictionary;
import org.junit.Before;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class ReleaseServiceTest {

  private ReleaseService releaseService;

  private Release release;

  final private String testDbName = "testDb";

  @Before
  public void setUp() {
    try {
      // use local host as test MongoDB for now
      Mongo mongo = new Mongo("localhost");
      Morphia morphia = new Morphia();
      morphia.map(BaseEntity.class);
      Datastore ds = morphia.createDatastore(mongo, testDbName);

      // Clear out the test database before each test
      ds.delete(ds.createQuery(Release.class));
      ds.delete(ds.createQuery(Project.class));

      // Set up a minimal test case
      release = new Release();
      Project project = new Project();
      Submission submission = new Submission();

      submission.setState(SubmissionState.VALID);
      submission.setProjectKey(project.getProjectKey());

      release.getSubmissions().add(submission);
      release.setDictionary(new Dictionary());

      // Create the releaseService and populate it with the initial release
      releaseService = new ReleaseService(morphia, ds);
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

  @Test
  public void test_getNextRelease_isCorrectRelease() {
    assertEquals(release.getId(), releaseService.getNextRelease().getRelease().getId());

    Release newRelease = addNewRelease();

    assertEquals(newRelease.getId(), releaseService.getNextRelease().getRelease().getId());
  }

  @Test
  public void test_getCompletedReleases_isCorrectSize() {
    assertEquals(0, releaseService.getCompletedReleases().size());

    addNewRelease();

    assertEquals(1, releaseService.getCompletedReleases().size());
  }

  @Test
  public void test_list_isCorrectSize() {
    assertEquals(1, releaseService.list().size());

    addNewRelease();

    assertEquals(2, releaseService.list().size());
  }

  private Release addNewRelease() {
    Release newRelease = new Release();
    releaseService.getNextRelease().release(newRelease);
    return newRelease;
  }
}
