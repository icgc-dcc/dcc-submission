package org.icgc.dcc.http;

import org.glassfish.grizzly.http.server.HttpHandler;

/**
 * An interface for things to mount on the {@code HttpServer}. Implementations should provide the path on which the
 * {@code HttpHandler} should be mounted on.
 */
public interface HttpHandlerProvider {

  /**
   * Constant representing the root
   */
  public String ROOT = "/";

  public String path();

  public HttpHandler get();

}
