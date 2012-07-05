package org.icgc.dcc.core.model;

import java.util.ArrayList;
import java.util.List;


import com.google.code.morphia.annotations.Entity;

@Entity
public class User extends BaseEntity implements HasName {

  protected String username;

  protected List<String> roles = new ArrayList<String>();

  public boolean hasRole(String role) {
    return this.roles.contains(role);
  }

  @Override
  public String getName() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public List<String> getRoles() {
    return roles;
  }
}
