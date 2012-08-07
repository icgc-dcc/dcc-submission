package org.icgc.dcc.shiro;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import org.apache.shiro.subject.Subject;
import org.icgc.dcc.security.BasicPrincipal;

public class ShiroSecurityContext implements SecurityContext {

  private final Subject subject;

  private final boolean isSecure;

  public ShiroSecurityContext(Subject subject, boolean isSecure) {
    this.subject = subject;
    this.isSecure = isSecure;
  }

  public Subject getSubject() {
    return subject;
  }

  @Override
  public Principal getUserPrincipal() {
    return new BasicPrincipal(this.subject.getPrincipal().toString());
  }

  @Override
  public boolean isUserInRole(String role) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isSecure() {
    return isSecure;
  }

  @Override
  public String getAuthenticationScheme() {
    return "DCC_AUTH";
  }

}
