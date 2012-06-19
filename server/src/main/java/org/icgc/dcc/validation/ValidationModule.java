package org.icgc.dcc.validation;

import org.icgc.dcc.validation.restriction.DiscreteValuesPipeExtender;
import org.icgc.dcc.validation.restriction.ForeingKeyFieldRestriction;
import org.icgc.dcc.validation.restriction.RangeFieldRestriction;
import org.icgc.dcc.validation.restriction.ValueTypeFieldRestriction;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class ValidationModule extends AbstractModule {

  private Multibinder<RestrictionType> types;

  @Override
  protected void configure() {
    types = Multibinder.newSetBinder(binder(), RestrictionType.class);

    bindRestriction(ForeingKeyFieldRestriction.Type.class);
    bindRestriction(DiscreteValuesPipeExtender.Type.class);
    bindRestriction(RangeFieldRestriction.Type.class);
    bindRestriction(ValueTypeFieldRestriction.Type.class);
  }

  private void bindRestriction(Class<? extends RestrictionType> f) {
    types.addBinding().to(f).in(Singleton.class);
  }

}
