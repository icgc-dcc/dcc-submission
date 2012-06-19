package org.icgc.dcc.validation;

import org.icgc.dcc.validation.restriction.DiscreteValuesFieldRestriction;
import org.icgc.dcc.validation.restriction.ForeingKeyFieldRestriction;
import org.icgc.dcc.validation.restriction.RangeFieldRestriction;
import org.icgc.dcc.validation.restriction.ValueTypeFieldRestriction;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class ValidationModule extends AbstractModule {

  private Multibinder<FieldRestrictionType> types;

  @Override
  protected void configure() {
    types = Multibinder.newSetBinder(binder(), FieldRestrictionType.class);

    bindRestriction(ForeingKeyFieldRestriction.Factory.class);
    bindRestriction(DiscreteValuesFieldRestriction.Type.class);
    bindRestriction(RangeFieldRestriction.Type.class);
    bindRestriction(ValueTypeFieldRestriction.Type.class);
  }

  private void bindRestriction(Class<? extends FieldRestrictionType> f) {
    types.addBinding().to(f).in(Singleton.class);
  }

}
