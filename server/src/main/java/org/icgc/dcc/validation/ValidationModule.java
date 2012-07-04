package org.icgc.dcc.validation;

import java.io.File;

import org.icgc.dcc.validation.restriction.CodeListRestriction;
import org.icgc.dcc.validation.restriction.DiscreteValuesRestriction;
import org.icgc.dcc.validation.restriction.RangeFieldRestriction;
import org.icgc.dcc.validation.restriction.RequiredRestriction;

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

    bindRestriction(DiscreteValuesRestriction.Type.class);
    bindRestriction(RangeFieldRestriction.Type.class);
    bindRestriction(RequiredRestriction.Type.class);
    bindRestriction(CodeListRestriction.Type.class);

    bind(Planner.class).to(DefaultPlanner.class);
    bind(FileSchemaDirectory.class).toInstance(new LocalFileSchemaDirectory(root));
    bind(CascadingStrategy.class).toInstance(new LocalCascadingStrategy(root, output));
  }

  private void bindRestriction(Class<? extends RestrictionType> type) {
    types.addBinding().to(type).in(Singleton.class);
  }

}
