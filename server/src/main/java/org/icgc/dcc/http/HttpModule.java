package org.icgc.dcc.http;

import org.glassfish.grizzly.http.server.HttpServer;
import org.icgc.dcc.core.AbstractDccModule;

public class HttpModule extends AbstractDccModule {

  @Override
  protected void configure() {
    bind(HttpServer.class).toInstance(new HttpServer());
    bindService(HttpServerService.class);
  }
}
