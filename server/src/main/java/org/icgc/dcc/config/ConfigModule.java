package org.icgc.dcc.config;

import com.google.inject.AbstractModule;
import com.typesafe.config.Config;

public class ConfigModule extends AbstractModule {

  private final Config config;

  public ConfigModule(Config config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    bind(Config.class).toInstance(config);
  }
}
