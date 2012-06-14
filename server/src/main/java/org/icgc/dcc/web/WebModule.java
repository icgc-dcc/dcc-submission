package org.icgc.dcc.web;

import org.glassfish.jersey.media.json.JsonJacksonModule;
import org.glassfish.jersey.server.ResourceConfig;
import org.icgc.dcc.http.jersey.BasicHttpAuthenticationRequestFilter;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

public class WebModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(RootResources.class).asEagerSingleton();
  }

  /**
   * Used to register resources in {@code Jersey}. This is required because {@code Jersey} cannot use Guice to discover
   * resources.
   */
  public static class RootResources {
    @Inject
    public RootResources(ResourceConfig config) {
      config.addModules(new JsonJacksonModule());
      config.addClasses(DictionaryResource.class);
      config.addClasses(ProjectResource.class);
      config.addClasses(ReleaseResource.class);
      config.addClasses(NextReleaseResource.class);
      config.addClasses(BasicHttpAuthenticationRequestFilter.class);
    }
  }

}
