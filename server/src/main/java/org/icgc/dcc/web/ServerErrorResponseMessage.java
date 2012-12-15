package org.icgc.dcc.web;

public class ServerErrorResponseMessage {
  public String code;

  public Object[] parameters;

  public ServerErrorResponseMessage(ServerErrorCode code, Object... parameters) {
    this.code = code.getCode();
    this.parameters = parameters;
  }

  public ServerErrorResponseMessage(ServerErrorCode code) { // TODO: change this to accept ServerErrorCodeEnum instead
                                                            // (DCC-660)
    this.code = code.getCode();
    this.parameters = new Object[0];
  }
}
