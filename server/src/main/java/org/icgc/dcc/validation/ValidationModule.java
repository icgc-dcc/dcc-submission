package org.icgc.dcc.validation;

import java.io.File;

import com.google.inject.AbstractModule;

public class ValidationModule extends AbstractModule {

  private final File root;

  private final File output;

  public ValidationModule(File root, File output) {
    this.root = root;
    this.output = output;
  }

  @Override
  protected void configure() {
    bind(FileSchemaDirectory.class).toInstance(new LocalFileSchemaDirectory(root));
    bind(CascadingStrategy.class).toInstance(new LocalCascadingStrategy(root, output));
  }
}
