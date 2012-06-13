package org.icgc.dcc.model;

import java.util.List;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;

@Entity
public class Project extends BaseEntity implements HasName {

  @Indexed(unique = true)
  protected String key;

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

  public Project(String name, String projectKey) {
    super();
    this.setName(name);
    this.setProjectKey(projectKey);
  }

  @Override
  public String getName() {
    return name;
  }

  public String getProjectKey() {
    return key;
  }

  public void setProjectKey(String projectKey) {
    this.key = projectKey;
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
