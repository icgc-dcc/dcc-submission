package org.icgc.dcc.shiro;

import java.util.Collection;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.realm.Realm;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SecurityManagerProvider implements Provider<org.apache.shiro.mgt.SecurityManager> {

  @Inject
  private Collection<Realm> realms;

  @Override
  public org.apache.shiro.mgt.SecurityManager get() {
    DefaultSecurityManager defaultSecurityManager = new DefaultSecurityManager(this.realms);
    disableSessions(defaultSecurityManager);
    SecurityUtils.setSecurityManager(defaultSecurityManager);
    return defaultSecurityManager;
  }

  /**
   * Disables server-side sessions entirely
   */
  private void disableSessions(DefaultSecurityManager defaultSecurityManager) {
    DefaultSubjectDAO subjectDao = (DefaultSubjectDAO) defaultSecurityManager.getSubjectDAO();
    ((DefaultSessionStorageEvaluator) subjectDao.getSessionStorageEvaluator()).setSessionStorageEnabled(false);
  }
}
