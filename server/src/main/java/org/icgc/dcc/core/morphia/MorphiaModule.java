package org.icgc.dcc.core.morphia;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.logging.MorphiaLoggerFactory;
import com.google.code.morphia.logging.slf4j.SLF4JLogrImplFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.typesafe.config.Config;

public class MorphiaModule extends AbstractModule {
  private static final Logger log = LoggerFactory.getLogger(MorphiaModule.class);

  @Override
  protected void configure() {
    // Use SLF4J with Morphia
    MorphiaLoggerFactory.reset();
    MorphiaLoggerFactory.registerLogger(SLF4JLogrImplFactory.class);

    bind(Morphia.class).toInstance(new Morphia());

    bind(Mongo.class).toProvider(new Provider<Mongo>() {

      @Inject
      private Config config;

      @Override
      public Mongo get() {
        try {
          String uri = config.getString("mongo.uri");
          log.info("mongo URI: {}", uri);
          return new MongoURI(uri).connect();
        } catch(Exception e) {
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
        MongoURI uri = new MongoURI(config.getString("mongo.uri"));
        log.info("mongo URI: {}", uri);
        Datastore datastore = morphia.createDatastore(mongo, uri.getDatabase());
        datastore.ensureIndexes();
        return datastore;
      }
    }).in(Singleton.class);
  }

}
