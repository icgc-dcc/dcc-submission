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

public class BaseEntityTest {

  @Test(groups = { "mongodb" })
  public void test() {
    try {
      // use local host as test MongoDB for now
      Mongo mongo = new Mongo("localhost");
      Morphia morphia = new Morphia();
      morphia.map(BaseEntity.class);
      Datastore ds = morphia.createDatastore(mongo, "testDB");

      // save base Entity to mongoDB
      BaseEntity baseEntity = new BaseEntity();
      ds.save(baseEntity);

      // load base entity from mongoDB
      ObjectId entityID = baseEntity.getId();
      BaseEntity entity = ds.get(BaseEntity.class, entityID);

      // check if baseEntity is saved to mongoDB
      assertEquals(baseEntity.getId(), entity.getId());
      assertEquals(baseEntity.getCreated(), entity.getCreated());
      assertEquals(baseEntity.getLastUpdate(), entity.getLastUpdate());

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
