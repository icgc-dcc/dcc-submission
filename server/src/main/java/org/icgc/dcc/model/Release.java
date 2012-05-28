package org.icgc.dcc.model;

import java.util.List;

import com.google.code.morphia.annotations.Entity;

@Entity
public class Release extends BaseEntity implements HasName {

  protected String name;

  protected ReleaseState state;

  protected List<Submission> submissions;

  @Override
  public String getName() {
    return name;
  }
}
