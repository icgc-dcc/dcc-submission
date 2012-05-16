package org.icgc.dcc.model;

import com.google.code.morphia.Morphia;
import com.google.inject.AbstractModule;
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
          return new Mongo("localhost");
        } catch(Exception e) {
          throw new RuntimeException(e);
        }
      }
    }).in(Singleton.class);
    bind(Morphia.class).toProvider(new Provider<Morphia>(){
      @Override
      public Morphia get() {
        return new Morphia().map(Project.class);
      }}).in(Singleton.class);
  }

}
