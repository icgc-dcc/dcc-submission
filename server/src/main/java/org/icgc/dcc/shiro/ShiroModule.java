package org.icgc.dcc.shiro;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;

import com.google.inject.AbstractModule;

public class ShiroModule extends AbstractModule {

  @Override
  protected void configure() {
    final DccRealm dccRealm = new DccRealm(); // TODO: put in provider?
    bind(Realm.class).toInstance(dccRealm);
    bind(SecurityManager.class).toInstance(new DefaultSecurityManager(dccRealm));
  }
}
