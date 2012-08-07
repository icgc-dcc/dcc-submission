package org.icgc.dcc.security;

import java.security.Principal;

public class BasicPrincipal implements Principal {

  private final String name;

  public BasicPrincipal(String username) {
    this.name = username;
  }

  @Override
  public String getName() {
    return name;
  }

}
