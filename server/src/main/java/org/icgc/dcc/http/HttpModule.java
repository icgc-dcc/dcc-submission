package org.icgc.dcc.http;

import org.glassfish.grizzly.http.server.HttpServer;

import com.google.inject.AbstractModule;

public class HttpModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(HttpServer.class).toInstance(new HttpServer());
    bind(HttpServerService.class);
  }
}
