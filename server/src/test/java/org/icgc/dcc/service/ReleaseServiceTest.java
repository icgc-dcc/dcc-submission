package org.icgc.dcc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;

import org.icgc.dcc.model.BaseEntity;
import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;
import org.testng.annotations.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class ReleaseServiceTest {

  @Test(groups = { "mongodb" })
  public void test() {
    Mongo mongo;
    try {
      mongo = new Mongo("localhost");
      Morphia morphia = new Morphia();
      morphia.map(BaseEntity.class);
      Datastore ds = morphia.createDatastore(mongo, "testDB");

      Release release = new Release("release");

      Project project = new Project("project", "1234");

      Submission submission = new Submission();
      submission.setState(SubmissionState.VALID);
      submission.setProject(project);

      release.getSubmissions().add(submission);

      ds.save(release);

      ReleaseService releaseService = new ReleaseService(morphia, ds);

      assertEquals(releaseService.getNextRelease().getRelease().getName(), release.getName());
      assertEquals(releaseService.getCompletedReleases().size(), 0);
      assertEquals(releaseService.list().size(), 1);

      Release newRelease = new Release("nextRelease");
      releaseService.getNextRelease().release(newRelease);

      assertEquals(releaseService.getNextRelease().getRelease().getName(), newRelease.getName());
      assertEquals(releaseService.getCompletedReleases().size(), 1);
      assertEquals(releaseService.list().size(), 2);

    } catch(UnknownHostException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch(MongoException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

  }

}
