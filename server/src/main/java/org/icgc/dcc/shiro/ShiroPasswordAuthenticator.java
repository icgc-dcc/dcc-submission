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

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.icgc.dcc.core.UserService;
import org.icgc.dcc.core.model.User;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;

/**
 * Implements {@code UsernamePasswordAuthenticator} on top of {@code Shiro}.
 */
public class ShiroPasswordAuthenticator implements UsernamePasswordAuthenticator {

  private static final Logger log = LoggerFactory.getLogger(ShiroPasswordAuthenticator.class);

  private final UserService users;

  /**
   * Somehow MUST inject {@code org.apache.shiro.mgt.SecurityManager} here to avoid the error message:
   * "HTTP/1.1 500 No SecurityManager accessible to the calling code, either bound to the org.apache.shiro.util.ThreadContext or as a vm static singleton.  This is an invalid application configuration."
   * TODO: find out why (DCC-?)
   */
  @Inject
  public ShiroPasswordAuthenticator(org.apache.shiro.mgt.SecurityManager securityManager, UserService users) {
    this.users = users;
  }

  /**
   * Saves user if encountered from the first time in database. TODO: revisit (will save all script kiddies username
   * attempts)?
   */
  @Override
  public Subject authenticate(final String username, final char[] password, final String host) {
    log.debug("Authenticating user {}", username);

    Optional<User> optionalUser = users.getUserByUsername(username);
    boolean newUser = optionalUser.isPresent() == false;

    User user;
    if(newUser) {
      user = new User(); // roles to be added along with saving after login (when subject will be linked to username)
      user.setUsername(username);
    } else {
      user = optionalUser.get();
    }

    // This code block is meant as a temporary solution for the need to disable user access after three failed
    // authentication attempts. It should be removed when the crowd server is enabled; see DCC-815
    if(user.isLocked()) {
      log.info("User " + username + " is locked. Please contact your administrator.");
      return null;
    }
    // END HACK

    // @formatter:off
    /*
     * "TO DO" below is cryptic, but removing this line produces the following symptom: can login with wrong password
     * provided that someone logged in successfully and logged out (even with a hard refresh)
     * 
     * To reproduce:
     * - comment out the line
     * - login with admin
     * - logout
     * - hard refresh
     * - attempt to login with a dummy password, it works every other time or so
     * - conversely, uncomment the line and it appears one can't login with the dummy password anymore (tried 3 times)
     */
    // @formatter:on
    ThreadContext.remove(); // TODO remove this once it is correctly done when the response is sent out (see DCC-815)
    Subject subject = null;
    try {
      subject = SecurityUtils.getSubject();
    } catch(UnavailableSecurityManagerException e) {
      log.error("Failure to get the current Subject:", e);
      Throwables.propagate(e);
    }

    // build token from credentials
    UsernamePasswordToken token = new UsernamePasswordToken(username, password, false, host);
    try {
      // attempt to login user
      subject.login(token);
    } catch(UnknownAccountException uae) {
      log.info("There is no user with username of {}", token.getPrincipal());
    } catch(IncorrectCredentialsException ice) {
      log.info("Password for account {} was incorrect!", token.getPrincipal());
    } catch(LockedAccountException lae) { // TODO: look into this rather than using above hack?
      log.info("The account for username {} is locked. Please contact your administrator to unlock it.",
          token.getPrincipal());
    } catch(AuthenticationException ae) { // FIXME: it seems invalid credentials actually result in:
                                          // org.apache.shiro.authc.AuthenticationException: Authentication token of
                                          // type [class org.apache.shiro.authc.UsernamePasswordToken] could not be
                                          // authenticated by any configured realms. Please ensure that at least one
                                          // realm can authenticate these tokens. (not IncorrectCredentialsException)
      log.error("Unknown error logging in {}. Please contact your administrator.", token.getPrincipal());
    }

    if(newUser) {
      users.saveUser(user);
    }

    if(subject.isAuthenticated()) {
      if(newUser == false) {
        users.resetUser(user); // Part of lockout hack
      }
      log.info("User [{}] logged in successfully.", subject.getPrincipal());
      return subject;
    } else {
      // Hack
      users.reprimandUser(user);
      log.info(user.isLocked() ? "user {} was locked after too many failed attempts" : //
      "user {} was reprimanded after a failed attempt", username);
      // End hack
      return null;
    }
  }

  @Override
  public Subject getSubject() {
    return SecurityUtils.getSubject();
  }
}
