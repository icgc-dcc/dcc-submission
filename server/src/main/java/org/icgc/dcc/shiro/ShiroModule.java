package org.icgc.dcc.shiro;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;

import com.google.inject.AbstractModule;

public class ShiroModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Realm.class).toProvider(RealmProvider.class);
    bind(SecurityManager.class).toProvider(SecurityManagerProvider.class);
  }
}
