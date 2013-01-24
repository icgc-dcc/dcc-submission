package org.icgc.dcc.web;

public class ServerErrorResponseMessage {
  public String code;

  public Object[] parameters;

  public ServerErrorResponseMessage(ServerErrorCode code, Object... parameters) {
    this.code = code.getFrontEndString();
    this.parameters = parameters;
  }

  public ServerErrorResponseMessage(ServerErrorCode code) {
    this.code = code.getFrontEndString();
    this.parameters = new Object[0];
  }
}
