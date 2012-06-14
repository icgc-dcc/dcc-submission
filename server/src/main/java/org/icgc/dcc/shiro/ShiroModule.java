package org.icgc.dcc.shiro;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class ShiroModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Realm.class).toProvider(RealmProvider.class);
    bind(SecurityManager.class).toProvider(SecurityManagerProvider.class).in(Singleton.class);
    bind(UsernamePasswordAuthenticator.class).to(ShiroPasswordAuthenticator.class);
  }
}
