package org.icgc.dcc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;

import org.bson.types.ObjectId;
import org.testng.annotations.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class ReleaseTest {

  @Test(groups = { "mongodb" })
  public void test() {
    try {
      // use local host as test MongoDB for now
      Mongo mongo = new Mongo("localhost");
      Morphia morphia = new Morphia();
      morphia.map(Release.class);
      Datastore ds = morphia.createDatastore(mongo, "testDB");

      // save base Entity to mongoDB
      Release release = new Release();
      release.setName("release");
      release.setState(ReleaseState.OPENED);

      Project project = new Project();
      project.setName("project");
      project.setAccessionId("1234");

      Submission submission = new Submission();
      submission.setState(SubmissionState.VALID);
      submission.setProject(project);

      release.getSubmissions().add(submission);

      ds.save(release);

      // load base entity from mongoDB
      ObjectId entityID = release.getId();
      Release releaseDB = ds.get(Release.class, entityID);

      // check release object
      assertEquals(release.getSubmissions().size(), releaseDB.getSubmissions().size());
      assertEquals(release.getName(), releaseDB.getName());
      assertEquals(release.getState(), releaseDB.getState());

      Submission submissionDB = releaseDB.getSubmissions().get(0);
      assertEquals(submission.getState(), submissionDB.getState());

      Project projectDB = submissionDB.getProject();
      assertEquals(project.getName(), projectDB.getName());
      assertEquals(project.getAccessionId(), projectDB.getAccessionId());

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

}
