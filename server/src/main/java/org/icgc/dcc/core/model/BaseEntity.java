package org.icgc.dcc.core.model;

import java.util.Date;

import org.bson.types.ObjectId;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.code.morphia.annotations.Id;

public class BaseEntity extends Timestamped implements HasId {

  @Id
  @JsonIgnore
  protected ObjectId id;

  protected BaseEntity() {
    this.created = new Date();
    this.lastUpdate = this.created;
  }

  @Override
  public ObjectId getId() {
    return id;
  }
}
