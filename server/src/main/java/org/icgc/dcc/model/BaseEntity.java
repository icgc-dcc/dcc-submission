package org.icgc.dcc.model;

import java.util.Date;

import org.bson.types.ObjectId;
import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

@Entity
public class BaseEntity extends Timestamped implements HasId {

  public BaseEntity() {
    this.created = new Date();
  }

  @Id
  @JsonIgnore
  protected ObjectId id;

  @Override
  public ObjectId getId() {
    return id;
  }
}
