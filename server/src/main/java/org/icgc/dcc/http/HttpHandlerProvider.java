package org.icgc.dcc.http;

import org.glassfish.grizzly.http.server.HttpHandler;

public interface HttpHandlerProvider {

  public String ROOT = "/";

  public String path();

  public HttpHandler get();
}
