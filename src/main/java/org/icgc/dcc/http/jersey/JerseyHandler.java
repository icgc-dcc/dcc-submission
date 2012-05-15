package org.icgc.dcc.http.jersey;


import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.jersey.grizzly2.GrizzlyHttpContainerProvider;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.icgc.dcc.http.HttpHandlerProvider;


public class JerseyHandler implements HttpHandlerProvider {

  private final ResourceConfig resourceConfig;

  @Inject
  public JerseyHandler(Config config, ResourceConfig resourceConfig) {
    this.resourceConfig = resourceConfig;
  }

  @Override
  public String path() {
    return "/ws";
  }

  @Override
  public HttpHandler get() {
    return new GrizzlyHttpContainerProvider().createContainer(HttpHandler.class, new ApplicationHandler(resourceConfig));
  }
}
