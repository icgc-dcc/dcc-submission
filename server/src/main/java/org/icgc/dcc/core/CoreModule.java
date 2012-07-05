package org.icgc.dcc.core;

import java.util.logging.LogManager;

import org.slf4j.bridge.SLF4JBridgeHandler;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class CoreModule extends AbstractModule {

  public CoreModule() {
    // Reset java.util.logging settings
    LogManager.getLogManager().reset();
    // Redirect java.util.logging to SLF4J
    SLF4JBridgeHandler.install();
  }

  @Override
  protected void configure() {
    bind(DccRuntime.class).in(Singleton.class);
    bind(ProjectService.class).in(Singleton.class);
    bind(UserService.class).in(Singleton.class);
  }

}
