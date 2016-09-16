package org.icgc.dcc.submission.server.config;

import java.net.UnknownHostException;

import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.logging.slf4j.SLF4JLogrImplFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class PersistenceConfig {

  {
    // Use SLF4J with Morphia
    MorphiaLoggerFactory.reset();
    MorphiaLoggerFactory.registerLogger(SLF4JLogrImplFactory.class);
  }

  @Bean
  public Morphia morphia() {
    return new Morphia();
  }

  @Bean
  public Mongo mongo(SubmissionProperties properties) throws UnknownHostException {
    val uri = new MongoClientURI(properties.getMongo().getUri());
    log.info("Creating mongo with URI: {}", uri);

    return new MongoClient(uri);
  }

  @Bean
  public Datastore datastore(SubmissionProperties properties, Mongo mongo, Morphia morphia) {
    val uri = new MongoClientURI(properties.getMongo().getUri());
    log.info("Creating datastore with mongo URI: {}", uri);

    val datastore = morphia.createDatastore(mongo, uri.getDatabase());
    datastore.ensureIndexes();

    return datastore;
  }

}
