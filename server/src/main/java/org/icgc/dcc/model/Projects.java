package org.icgc.dcc.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.Predicate;

// TODO: make an abstract base class
public class Projects {

  private final Morphia morphia;

  private final Datastore datastore;

  @Inject
  public Projects(Morphia morphia, Datastore datastore) {
    super();
    checkArgument(morphia != null);
    checkArgument(datastore != null);
    this.morphia = morphia;
    this.datastore = datastore;
  }

  public Datastore datastore() {
    return datastore;
  }

  public MongodbQuery<Project> query() {
    return new MorphiaQuery<Project>(morphia, datastore, QProject.project);
  }

  public MongodbQuery<Project> where(Predicate predicate) {
    return query().where(predicate);
  }

}
