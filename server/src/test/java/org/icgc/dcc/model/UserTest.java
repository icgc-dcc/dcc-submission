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

public class UserTest {

  @Test(groups = { "mongodb" })
  public void test() {
    try {
      // use local host as test MongoDB for now
      Mongo mongo = new Mongo("localhost");
      Morphia morphia = new Morphia();
      morphia.map(User.class);
      Datastore ds = morphia.createDatastore(mongo, "testDB");

      // save base Entity to mongoDB
      User user = new User();
      user.setUsername("user");
      user.getRoles().add("admin");
      ds.save(user);

      // load base entity from mongoDB
      ObjectId entityID = user.getId();
      User userDB = ds.get(User.class, entityID);

      assertEquals(user.getId(), userDB.getId());
      assertEquals(user.getName(), userDB.getName());
      assertEquals(user.getRoles().size(), userDB.getRoles().size());

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
