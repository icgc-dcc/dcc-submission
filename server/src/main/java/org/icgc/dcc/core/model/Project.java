package org.icgc.dcc.core.model;

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

  public Project(String name, String key) {
    super();
    this.setName(name);
    this.setKey(key);
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public List<String> getUsers() {
    return users;
  }

  public void setUsers(List<String> users) {
    this.users = users;
  }

  public List<String> getGroups() {
    return groups;
  }

  public void setGroups(List<String> groups) {
    this.groups = groups;
  }

  public boolean hasUser(String name) {
    return this.users != null && this.users.contains(name);
  }
}
