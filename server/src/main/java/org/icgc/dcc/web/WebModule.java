package org.icgc.dcc.web;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import org.glassfish.jersey.server.ResourceConfig;

public class WebModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(RootResources.class).asEagerSingleton();
  }

  /**
   * Used to register resources in {@code Jersey}. This is required because {@code Jersey} cannot use Guice to discover resources.
   */
  public static class RootResources {
    @Inject
    public RootResources(ResourceConfig config) {
      config.addClasses(MyResource.class);
    }
  }


}
