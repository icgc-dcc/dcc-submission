package org.icgc.dcc.validation;

import java.io.File;

import org.icgc.dcc.validation.plan.CascadingStrategy;
import org.icgc.dcc.validation.plan.DefaultPlanner;
import org.icgc.dcc.validation.plan.LocalCascadingStrategy;
import org.icgc.dcc.validation.plan.Planner;
import org.icgc.dcc.validation.restriction.DiscreteValuesPipeExtender;
import org.icgc.dcc.validation.restriction.ForeingKeyFieldRestriction;
import org.icgc.dcc.validation.restriction.RangeFieldRestriction;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public class ValidationModule extends AbstractModule {

  private Multibinder<RestrictionType> types;

  private final File root;

  private final File output;

  public ValidationModule(File root, File output) {
    this.root = root;
    this.output = output;
  }

  @Override
  protected void configure() {
    types = Multibinder.newSetBinder(binder(), RestrictionType.class);

    bindRestriction(ForeingKeyFieldRestriction.Type.class);
    bindRestriction(DiscreteValuesPipeExtender.Type.class);
    bindRestriction(RangeFieldRestriction.Type.class);

    bind(Planner.class).to(DefaultPlanner.class);
    bind(CascadingStrategy.class).toInstance(new LocalCascadingStrategy(root, output));
  }

  private void bindRestriction(Class<? extends RestrictionType> f) {
    types.addBinding().to(f).in(Singleton.class);
  }

}
