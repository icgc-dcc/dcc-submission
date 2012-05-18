package org.icgc.dcc.model;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Id;
import com.google.code.morphia.annotations.Indexed;

@Entity
public class Project {

  @Id
  @JsonIgnore
  public String id;

  @Indexed(unique = true)
  public String accessionId;

  public String name;

}
