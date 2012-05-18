package org.icgc.dcc.http.jersey;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.sun.hk2.component.InjectionResolver;
import org.glassfish.jersey.server.ResourceConfig;
import org.jvnet.hk2.component.Inhabitant;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

/**
 * Used to register an {@code InjectionResolver} that will resolve Guice's {@code Inject} annotation.
 */
public class InjectModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(GuiceInjectResolver.class).asEagerSingleton();
    bind(GuiceModule.class).asEagerSingleton();
  }

  public static class GuiceInjectResolver extends InjectionResolver<Inject> {

    private final Injector injector;

    @Inject
    public GuiceInjectResolver(Injector injector) {
      super(Inject.class);
      this.injector = injector;
    }

    @Override
    public <V> V getValue(Object component, Inhabitant<?> onBehalfOf, AnnotatedElement annotated, Type genericType, Class<V> type) throws org.jvnet.hk2.component.ComponentException {
      return injector.getInstance(type);
    }
  }

  public static final class GuiceModule extends org.glassfish.jersey.internal.inject.AbstractModule {

    private final GuiceInjectResolver guiceResolver;

    @Inject
    public GuiceModule(GuiceInjectResolver guiceResolver, ResourceConfig config) {
      this.guiceResolver = guiceResolver;
      config.addModules(this);
    }

    @Override
    protected void configure() {
      bind(InjectionResolver.class).toInstance(guiceResolver);
    }
  }

}
