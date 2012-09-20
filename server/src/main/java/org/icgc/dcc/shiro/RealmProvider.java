package org.icgc.dcc.shiro;

import java.util.Collection;
import java.util.HashSet;

import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.text.IniRealm;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;

public class RealmProvider implements Provider<Collection<Realm>> {
  private static final Logger log = LoggerFactory.getLogger(RealmProvider.class);

  @Inject
  private Config config;

  @Inject
  private UserService users;

  @Inject
  private ProjectService projects;

  @Override
  public Collection<Realm> get() {
    Collection<Realm> realms = new HashSet<Realm>();

    String shiroIniFilePath = this.config.getString(ShiroConfig.SHIRO_INI_FILE);
    log.debug("shiroIniFilePath = " + shiroIniFilePath);

    IniRealm iniRealm = new IniRealm();
    iniRealm.setResourcePath("file:" + shiroIniFilePath);// TODO: existing constant for that?
    iniRealm.init();

    realms.add(iniRealm);

    DccDbRealm dccDbRealm = new DccDbRealm(users, projects);
    // TODO investigate caching particulars
    dccDbRealm.setAuthorizationCachingEnabled(false);
    realms.add(dccDbRealm);

    return realms;
  }
}
