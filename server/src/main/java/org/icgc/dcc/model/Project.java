package org.icgc.dcc.model;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;

@Entity
public class Project {

  @Id
  @JsonIgnore
  public String id;

  public String accessionId;

  public String name;

}
