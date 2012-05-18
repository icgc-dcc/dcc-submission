package org.icgc.dcc.model;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.mongodb.Mongo;

public class ModelModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Mongo.class).toProvider(new Provider<Mongo>() {

      @Override
      public Mongo get() {
        try {
          // TODO: read connection parameters from config
          return new Mongo("localhost");
        } catch(Exception e) {
          throw new RuntimeException(e);
        }
      }
    }).in(Singleton.class);

    bind(Datastore.class).toProvider(new Provider<Datastore>() {

      @Inject
      Mongo mongo;

      @Inject
      Morphia morphia;

      @Override
      public Datastore get() {
        // TODO: get database name from config
        return morphia.createDatastore(mongo, "icgc");
      }
    }).in(Singleton.class);

    bindModelClasses(Project.class);
    bind(Projects.class);
  }

  private void bindModelClasses(final Class<?>... models) {
    Morphia morphia = new Morphia();
    for(Class<?> model : models) {
      morphia.map(model);
    }
    bind(Morphia.class).toInstance(morphia);
  }

}
