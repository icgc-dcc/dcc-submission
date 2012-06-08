package org.icgc.dcc.model;

import java.util.List;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;

@Entity
public class Project extends BaseEntity implements HasName {

  @Indexed(unique = true)
  protected String accessionId;

  protected String name;

  protected List<String> users;

  protected List<String> groups;

  public Project() {
    super();
  }

  public Project(String name) {
    super();
    this.setName(name);
  }

  public Project(String name, String accessionID) {
    super();
    this.setName(name);
    this.setAccessionId(accessionID);
  }

  @Override
  public String getName() {
    return name;
  }

  public String getAccessionId() {
    return accessionId;
  }

  public void setAccessionId(String accessionId) {
    this.accessionId = accessionId;
  }

  public List<String> getUsers() {
    return users;
  }

  public List<String> getGroups() {
    return groups;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean hasUser(String name) {
    return this.users != null && this.users.contains(name);
  }
}
