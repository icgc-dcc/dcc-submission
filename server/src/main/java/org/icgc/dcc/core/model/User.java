package org.icgc.dcc.core.model;

import java.util.List;

import com.google.code.morphia.annotations.Entity;
import com.google.common.collect.Lists;

@Entity
public class User extends BaseEntity implements HasName {

  protected String username;

  protected List<String> roles = Lists.newArrayList();

  protected String email;

  public boolean hasRole(String role) {
    return this.roles.contains(role);
  }

  @Override
  public String getName() {
    return username;
  }

  public String getEmail() {
    return this.email;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public List<String> getRoles() {
    return roles;
  }
}
