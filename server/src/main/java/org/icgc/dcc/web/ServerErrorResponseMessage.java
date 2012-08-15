package org.icgc.dcc.web;

public class ServerErrorResponseMessage {
  public String code;

  public Object[] parameters;

  public ServerErrorResponseMessage(String code, Object... parameters) {
    this.code = code;
    this.parameters = parameters;
  }

  public ServerErrorResponseMessage(String code) {
    this.code = code;
    this.parameters = new Object[0];
  }
}
