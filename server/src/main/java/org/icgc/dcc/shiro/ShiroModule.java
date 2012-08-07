package org.icgc.dcc.shiro;

import java.util.Collection;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class ShiroModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<Collection<Realm>>() {
    }).toProvider(RealmProvider.class);
    bind(SecurityManager.class).toProvider(SecurityManagerProvider.class).in(Singleton.class);
    bind(UsernamePasswordAuthenticator.class).to(ShiroPasswordAuthenticator.class);
  }
}
