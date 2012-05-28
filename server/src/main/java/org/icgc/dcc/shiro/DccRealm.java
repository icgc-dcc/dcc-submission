package org.icgc.dcc.shiro;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.typesafe.config.Config;

/**
 * inspired from MyRealm.java (in Shiro's examples)
 * 
 * @author Anthony Cros (anthony.cros@oicr.on.ca)
 */
public class DccRealm extends AuthorizingRealm {
  private static final Logger log = LoggerFactory.getLogger(DccRealm.class);

  @Inject
  private Config config;

  private static final String SHIRO_USERS_CONFIG_SECTION = "shiro.users";

  protected SimpleAccount getAccount(String username) { // TODO: read all accounts only once and store in map
    log.debug("username = " + username);
    String password = this.config.getString(SHIRO_USERS_CONFIG_SECTION + "." + username + "." + "passwd");
    log.debug("password retrieved properly");
    SimpleAccount account = new SimpleAccount(username, password, getName());
    return account;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    String username = (String) getAvailablePrincipal(principals);
    return getAccount(username);
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    UsernamePasswordToken upToken = (UsernamePasswordToken) token;
    return getAccount(upToken.getUsername());
  }

}
