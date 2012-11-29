package org.icgc.dcc.web;

public class ServerErrorResponseMessage {
  public String code;

  public Object[] parameters;

  public ServerErrorResponseMessage(String code, Object... parameters) {
    this.code = code;
    this.parameters = parameters;
  }

  public ServerErrorResponseMessage(String code) { // TODO: change this to accept ServerErrorCodeEnum instead (DCC-660)
    this.code = code;
    this.parameters = new Object[0];
  }
}
