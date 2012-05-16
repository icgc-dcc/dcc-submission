package org.icgc.dcc.http.jersey;


import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainerProvider;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.icgc.dcc.http.HttpHandlerProvider;


/**
 * A {@link HttpHandlerProvider} that will mount {@code Jersey} on a particular path. The path is configured through the {@code http.ws.path} parameter.
 */
public class JerseyHandler implements HttpHandlerProvider {
  
  private final Config config;
  
  private final ResourceConfig resourceConfig;

  @Inject
  public JerseyHandler(Config config, ResourceConfig resourceConfig) {
    Preconditions.checkArgument(config != null);
    Preconditions.checkArgument(resourceConfig != null);
    this.config = config;
    this.resourceConfig = resourceConfig;
  }

  @Override
  public String path() {
    return config.getString("http.ws.path");
  }

  @Override
  public HttpHandler get() {
    return new GrizzlyHttpContainerProvider().createContainer(HttpHandler.class, new ApplicationHandler(resourceConfig));
  }
}
