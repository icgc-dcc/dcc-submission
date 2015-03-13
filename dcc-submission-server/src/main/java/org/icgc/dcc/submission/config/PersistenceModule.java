package org.icgc.dcc.submission.config;

import static org.icgc.dcc.common.core.model.Configurations.MONGO_URI_KEY;

import java.net.UnknownHostException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.logging.slf4j.SLF4JLogrImplFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.typesafe.config.Config;

@Slf4j
public class PersistenceModule extends AbstractModule {

  @Override
  protected void configure() {
    // Use SLF4J with Morphia
    MorphiaLoggerFactory.reset();
    MorphiaLoggerFactory.registerLogger(SLF4JLogrImplFactory.class);

    bind(Morphia.class).toInstance(new Morphia());
  }

  @Provides
  @Singleton
  public Mongo mongo(Config config) throws UnknownHostException {
    val uri = config.getString(MONGO_URI_KEY);
    log.info("mongo URI: {}", uri);
    return new MongoClient(new MongoClientURI(uri));
  }

  @Provides
  @Singleton
  public Datastore datastore(Config config, Mongo mongo, Morphia morphia) {
    val uri = new MongoClientURI(config.getString(MONGO_URI_KEY));
    log.info("mongo URI: {}", uri);

    val datastore = morphia.createDatastore(mongo, uri.getDatabase());
    datastore.ensureIndexes();

    return datastore;
  }

}
