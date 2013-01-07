package org.icgc.dcc.web;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.glassfish.jersey.server.ResourceConfig;
import org.icgc.dcc.http.jersey.BasicHttpAuthenticationRequestFilter;
import org.icgc.dcc.web.mapper.DuplicateNameExceptionMapper;
import org.icgc.dcc.web.mapper.InvalidNameExceptionMapper;
import org.icgc.dcc.web.mapper.ReleaseExceptionMapper;
import org.icgc.dcc.web.mapper.UnsatisfiedPreconditionExceptionMapper;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;

public class WebModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(RootResources.class).asEagerSingleton();
    bind(Validator.class).toInstance(Validation.buildDefaultValidatorFactory().getValidator());
  }

  /**
   * Used to register resources in {@code Jersey}. This is required because {@code Jersey} cannot use Guice to discover
   * resources.
   */
  public static class RootResources {
    @SuppressWarnings("unchecked")
    @Inject
    public RootResources(ResourceConfig config) {
      config.register(ValidatingJacksonJsonProvider.class, MessageBodyReader.class, MessageBodyWriter.class);

      config.addClasses(ProjectResource.class);
      config.addClasses(ReleaseResource.class);
      config.addClasses(NextReleaseResource.class);
      config.addClasses(DictionaryResource.class);
      config.addClasses(CodeListResource.class);
      config.addClasses(BasicHttpAuthenticationRequestFilter.class);
      config.addClasses(UnsatisfiedPreconditionExceptionMapper.class);
      config.addClasses(ReleaseExceptionMapper.class);
      config.addClasses(InvalidNameExceptionMapper.class);
      config.addClasses(DuplicateNameExceptionMapper.class);
      config.addClasses(UserResource.class);
      config.addClasses(SeedResource.class); // TODO be sure to remove this from production environment
    }
  }

}
