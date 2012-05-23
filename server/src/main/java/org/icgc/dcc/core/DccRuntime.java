package org.icgc.dcc.core;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;

public class DccRuntime {

  private static final Logger log = LoggerFactory.getLogger(DccRuntime.class);

  private final Set<Service> services;

  @Inject
  public DccRuntime(Set<Service> services) {
    this.services = services;
  }

  public void start() {
    for(Service service : services) {
      tryStartService(service);
    }
  }

  public void stop() {
    for(Service service : services) {
      tryStopService(service);
    }
  }

  private void tryStopService(Service service) {
    try {
      log.info("Service {} is [{}]. Stoping.", service.getClass(), service.state());
      service.stopAndWait();
      log.info("Service {} is now [{}]", service.getClass(), service.state());
    } catch(UncheckedExecutionException e) {
      log.warn("Failed to stop service {}: {}", service.getClass(), e.getCause().getMessage());
    }
  }

  private void tryStartService(Service service) {
    try {
      log.info("Service {} is [{}]. Starting.", service.getClass(), service.state());
      service.startAndWait();
      log.info("Service {} is now [{}]", service.getClass(), service.state());
    } catch(UncheckedExecutionException e) {
      log.warn("Failed to start service {}: {}", service.getClass(), e.getCause().getMessage());
    }
  }

}
