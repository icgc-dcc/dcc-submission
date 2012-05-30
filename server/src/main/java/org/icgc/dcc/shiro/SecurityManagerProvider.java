package org.icgc.dcc.shiro;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SecurityManagerProvider implements Provider<org.apache.shiro.mgt.SecurityManager> {

  @Inject
  private Realm realm;

  @Override
  public org.apache.shiro.mgt.SecurityManager get() {
    DefaultSecurityManager defaultSecurityManager = new DefaultSecurityManager(this.realm);
    SecurityUtils.setSecurityManager(defaultSecurityManager);
    return defaultSecurityManager;
  }
}
