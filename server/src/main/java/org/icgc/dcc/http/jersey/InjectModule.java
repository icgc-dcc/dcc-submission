package org.icgc.dcc.http.jersey;

import java.lang.reflect.Type;

import javax.inject.Singleton;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Used to register an {@code InjectionResolver} that will resolve Guice's {@code Inject} annotation.
 */
public class InjectModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(GuiceInjectResolver.class).asEagerSingleton();
    bind(GuiceModule.class).asEagerSingleton();
  }

  @Singleton
  public static class GuiceInjectResolver implements InjectionResolver<Inject> {

    private final Injector injector;

    @Inject
    public GuiceInjectResolver(Injector injector) {
      this.injector = injector;
    }

    @Override
    public boolean isConstructorParameterIndicator() {
      return true;
    }

    @Override
    public boolean isMethodParameterIndicator() {
      return false;
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
      Type type = injectee.getRequiredType();
      if(type instanceof Class) {
        return injector.getInstance((Class) type);
      }
      throw new IllegalStateException("don't know how to inject type " + type);
    }

  }

  public static final class GuiceModule extends AbstractBinder {

    private final GuiceInjectResolver guiceResolver;

    @Inject
    public GuiceModule(GuiceInjectResolver guiceResolver, ResourceConfig config) {
      this.guiceResolver = guiceResolver;
      config.addBinders(this);
    }

    @Override
    protected void configure() {
      bind(guiceResolver).to(InjectionResolver.class);
    }
  }

}
