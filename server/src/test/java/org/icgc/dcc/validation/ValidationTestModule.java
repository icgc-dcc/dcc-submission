package org.icgc.dcc.validation;

import static org.mockito.Mockito.mock;

import org.icgc.dcc.core.AbstractDccModule;
import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.validation.restriction.CodeListRestriction;
import org.icgc.dcc.validation.restriction.DiscreteValuesRestriction;
import org.icgc.dcc.validation.restriction.RangeFieldRestriction;
import org.icgc.dcc.validation.restriction.RequiredRestriction;

import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class ValidationTestModule extends AbstractDccModule {

  private Multibinder<RestrictionType> types;

  @Override
  protected void configure() {
    bind(DictionaryService.class).toInstance(mock(DictionaryService.class));

    bind(Planner.class).to(DefaultPlanner.class);
    types = Multibinder.newSetBinder(binder(), RestrictionType.class);

    bindRestriction(DiscreteValuesRestriction.Type.class);
    bindRestriction(RangeFieldRestriction.Type.class);
    bindRestriction(RequiredRestriction.Type.class);
    bindRestriction(CodeListRestriction.Type.class);
  }

  private void bindRestriction(Class<? extends RestrictionType> type) {
    types.addBinding().to(type).in(Singleton.class);
  }
}
