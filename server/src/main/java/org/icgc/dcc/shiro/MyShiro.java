package org.icgc.dcc.shiro;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.typesafe.config.Config;

/**
 * dummy class (from Shiro's Quickstart.java) to test if config is properly loaded and shiro works
 * 
 * @author Anthony Cros (anthony.cros@oicr.on.ca)
 */
public class MyShiro {

  private static final Logger log = LoggerFactory.getLogger(MyShiro.class);

  @Inject
  private Config config;

  @SuppressWarnings("unused")
  @Inject
  private SecurityManager securityManager; // must inject it even if not used (not sure why)

  public void doIt() {

    // test can get some params
    log.info(config.getString("shiro.realm"));

    // Now that a simple Shiro environment is set up, let's see what you can do:

    // get the currently executing user:
    Subject currentUser = SecurityUtils.getSubject();

    // Do some stuff with a Session (no need for a web or EJB container!!!)
    Session session = currentUser.getSession();
    session.setAttribute("someKey", "aValue");
    String value = (String) session.getAttribute("someKey");
    if(value.equals("aValue")) {
      log.info("Retrieved the correct value! [" + value + "]");
    }

    // let's login the current user so we can check against roles and permissions:
    if(!currentUser.isAuthenticated()) {
      UsernamePasswordToken token = new UsernamePasswordToken("brett", "brettspasswd");
      token.setRememberMe(true);
      try {
        currentUser.login(token);
      } catch(UnknownAccountException uae) {
        log.info("There is no user with username of " + token.getPrincipal());
      } catch(IncorrectCredentialsException ice) {
        log.info("Password for account " + token.getPrincipal() + " was incorrect!");
      } catch(LockedAccountException lae) {
        log.info("The account for username " + token.getPrincipal() + " is locked.  "
            + "Please contact your administrator to unlock it.");
      }
      // ... catch more exceptions here (maybe custom ones specific to your application?
      catch(AuthenticationException ae) {
        // unexpected condition? error?
      }

      // say who they are:
      // print their identifying principal (in this case, a username):
      log.info("User [" + currentUser.getPrincipal() + "] logged in successfully.");
    }

    // all done - log out!
    currentUser.logout();

  }

}
