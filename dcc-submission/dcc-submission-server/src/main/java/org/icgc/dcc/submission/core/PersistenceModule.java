package org.icgc.dcc.submission.core;

import static org.icgc.dcc.core.model.Configurations.MONGO_URI_KEY;
import lombok.extern.slf4j.Slf4j;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.logging.slf4j.SLF4JLogrImplFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
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
    bind(Mongo.class).toProvider(new Provider<MongoClient>() {

      @Inject
      private Config config;

      @Override
      public MongoClient get() {
        try {
          String uri = config.getString(MONGO_URI_KEY);
          log.info("mongo URI: {}", uri);
          return new MongoClient(new MongoClientURI(uri));
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }).in(Singleton.class);

    bind(Datastore.class).toProvider(new Provider<Datastore>() {

      @Inject
      Config config;

      @Inject
      Mongo mongo;

      @Inject
      Morphia morphia;

      @Override
      public Datastore get() {
        MongoClientURI uri = new MongoClientURI(config.getString(MONGO_URI_KEY));
        log.info("mongo URI: {}", uri);
        Datastore datastore = morphia.createDatastore(mongo, uri.getDatabase());
        datastore.ensureIndexes();
        return datastore;
      }
    }).in(Singleton.class);

  }

}
