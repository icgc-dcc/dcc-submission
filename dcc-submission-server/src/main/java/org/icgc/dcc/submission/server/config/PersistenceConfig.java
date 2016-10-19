package org.icgc.dcc.submission.server.config;

import java.net.UnknownHostException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.logging.MorphiaLoggerFactory;
import org.mongodb.morphia.logging.slf4j.SLF4JLogrImplFactory;
import org.mongodb.morphia.mapping.MapperOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

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
    val morphia = new Morphia();
    val options = new MapperOptions();
    options.setStoreEmpties(true);
    morphia.getMapper().setOptions(options);

    return morphia;
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
