package org.icgc.dcc.http;

import com.google.inject.AbstractModule;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;

public class HttpModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(HttpServer.class).toInstance(new HttpServer());
    bind(DccHttpServer.class);
    bind(ResourceConfig.class).toInstance(new ResourceConfig());
  }
}
