package org.icgc.dcc.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;

import org.bson.types.ObjectId;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class ReleaseTest {

  @Test
  public void test() {
    try {
      // use local host as test MongoDB for now
      Mongo mongo = new Mongo("localhost");
      Morphia morphia = new Morphia();
      morphia.map(Release.class);
      Datastore ds = morphia.createDatastore(mongo, "testDB");

      // save base Entity to mongoDB
      Release release = new Release();

      Project project = new Project();

      Submission submission = new Submission();

      submission.setProject(project);
      release.getSubmissions().add(submission);

      ds.save(release);

      // load base entity from mongoDB
      ObjectId entityID = release.getId();
      Release releaseDB = ds.get(Release.class, entityID);

      assertEquals(release.getId(), releaseDB.getId());
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
