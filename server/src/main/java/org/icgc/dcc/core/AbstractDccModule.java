package org.icgc.dcc.core;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

public abstract class AbstractDccModule extends AbstractModule {

  /**
   * Creates a singleton binding for a {@code Service} class. This will allow managing the service's lifecycle
   * automatically.
   */
  protected void bindService(Class<? extends Service> serviceClass) {
    bind(serviceClass).in(Singleton.class);
    Multibinder<Service> servicesBinder = Multibinder.newSetBinder(binder(), Service.class);
    servicesBinder.addBinding().to(serviceClass);
  }

}
