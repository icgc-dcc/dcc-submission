package org.icgc.dcc.model;

import java.util.ArrayList;
import java.util.List;

import com.google.code.morphia.annotations.Entity;

@Entity
public class Release extends BaseEntity implements HasName {

  protected String name;

  protected ReleaseState state;

  protected List<Submission> submissions = new ArrayList<Submission>();

  public Release() {
    super();
    this.setState(ReleaseState.OPENED);
  }

  public Release(String name) {
    super();
    this.setName(name);
    this.setState(ReleaseState.OPENED);
  }

  @Override
  public String getName() {
    return name;
  }

  public ReleaseState getState() {
    return state;
  }

  public void setState(ReleaseState state) {
    this.state = state;
  }

  public List<Submission> getSubmissions() {
    return submissions;
  }

  public void setName(String name) {
    this.name = name;
  }
}
