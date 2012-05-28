package org.icgc.dcc.validation;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class ValidationModule extends AbstractModule {

  private Multibinder<FieldRestrictionFactory> factories;

  @Override
  protected void configure() {
    factories = Multibinder.newSetBinder(binder(), FieldRestrictionFactory.class);

    bindRestriction(ForeingKeyFieldRestriction.Factory.class);
  }

  private void bindRestriction(Class<? extends FieldRestrictionFactory> f) {
    factories.addBinding().to(f).in(Singleton.class);
  }

}
