package org.icgc.dcc.model;

import java.util.List;

import com.google.code.morphia.annotations.Embedded;
import com.google.code.morphia.annotations.Indexed;

@Embedded
public class Project extends BaseEntity implements HasName {

  @Indexed(unique = true)
  public String accessionId;

  public String name;

  protected List<String> users;

  protected List<String> groups;

  @Override
  public String getName() {
    return name;
  }

}
