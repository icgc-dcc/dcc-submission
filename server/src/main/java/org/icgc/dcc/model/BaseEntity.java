package org.icgc.dcc.model;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

@Entity
public class BaseEntity extends Timestamped implements HasId {

  @Id
  @JsonIgnore
  protected String id;

  @Override
  public String getId() {
    return id;
  }
}
