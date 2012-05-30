package org.icgc.dcc.shiro;

import org.apache.shiro.realm.Realm;
import org.apache.shiro.realm.text.IniRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;

public class RealmProvider implements Provider<Realm> {
  private static final Logger log = LoggerFactory.getLogger(RealmProvider.class);

  private static final String SHIRO_INI_FILE = "shiro.realm";

  @Inject
  private Config config;

  @Override
  public Realm get() {

    String shiroIniFilePath = this.config.getString(SHIRO_INI_FILE);
    log.debug("shiroIniFilePath = " + shiroIniFilePath);

    IniRealm iniRealm = new IniRealm();
    iniRealm.setResourcePath("classpath:" + shiroIniFilePath);
    iniRealm.init();

    return iniRealm;
  }
}
