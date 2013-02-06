/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.shiro;

import java.util.Collection;
import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.icgc.dcc.core.UserService;
import org.icgc.dcc.core.model.User;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * Implements {@code UsernamePasswordAuthenticator} on top of {@code Shiro}
 */
public class ShiroPasswordAuthenticator implements UsernamePasswordAuthenticator {

  private final SecurityManager securityManager;

  private final UserService users;

  @Inject
  public ShiroPasswordAuthenticator(SecurityManager securityManager, UserService users) {
    this.securityManager = securityManager;
    this.users = users;
  }

  private static final Logger log = LoggerFactory.getLogger(ShiroPasswordAuthenticator.class);

  @Override
  public Subject authenticate(final String username, final char[] password, final String host) {
    // This code block is meant as a temporary solution for the need to disable user access after three failed
    // authentication attempts. It should be removed when the crowd server is enabled
    User user = users.getUser(username);
    if(user.isLocked()) {
      log.info("User " + username + " is locked. Please contact your administrator.");
      return null;
    }
    // END HACK

    // build token from credentials
    UsernamePasswordToken token = new UsernamePasswordToken(username, password, false, host);

    ThreadContext.remove(); // TODO remove this once it is correctly done when the response is sent out
    Subject currentUser = SecurityUtils.getSubject();

    try {
      // attempt to login user
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
      log.info("Unknown error logging in " + token.getPrincipal() + "Please contact your administrator.");
    }

    if(currentUser.isAuthenticated()) {
      // say who they are:
      // print their identifying principal (in this case, a username):
      user.resetAttempts(); // Part of lockout hack
      users.saveUser(user); // Part of lockout hack - FIXME: use update instead (DCC-661)
      log.info("User [" + currentUser.getPrincipal() + "] logged in successfully.");
      return currentUser;
    }

    // Hack
    user.incrementAttempts();
    users.saveUser(user); // FIXME: use update instead? (DCC-661)
    if(user.isLocked()) {
      log.info("user {} was locked after too many failed attempts", username);
    }
    // End hack

    return null;
  }

  @Override
  public String getCurrentUser() {
    return SecurityUtils.getSubject().getPrincipal().toString();
  }

  @Override
  public Subject getSubject() {
    return SecurityUtils.getSubject();
  }

  @Override
  public List<String> getRoles() {
    List<String> roles = Lists.newArrayList();

    DefaultSecurityManager defaultSecurityManager = (DefaultSecurityManager) this.securityManager;
    for(Realm realm : defaultSecurityManager.getRealms()) {
      DccRealm dccRealm = (DccRealm) realm;
      Collection<String> iniRealmRoles = dccRealm.getRoles(this.getCurrentUser());
      roles.addAll(iniRealmRoles);
    }
    return roles;
  }
}
