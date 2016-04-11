/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.shiro;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.icgc.dcc.submission.core.model.User;
import org.icgc.dcc.submission.security.UsernamePasswordAuthenticator;
import org.icgc.dcc.submission.service.UserService;

import com.google.inject.Inject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Implements {@code UsernamePasswordAuthenticator} on top of {@code Shiro}.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ShiroPasswordAuthenticator implements UsernamePasswordAuthenticator {

  @NonNull
  private final UserService userService;

  /**
   * Saves user if encountered from the first time in database.
   */
  @Override
  public Subject authenticate(String username, char[] password, String host) {
    log.debug("Authenticating user {}", username);

    val optionalUser = userService.getUserByUsername(username);
    boolean newUser = optionalUser.isPresent() == false;

    User user;
    if (newUser) {
      // Roles to be added along with saving after login (when subject will be linked to username)
      user = new User();
      user.setUsername(username);
    } else {
      user = optionalUser.get();
    }

    if (user.isLocked()) {
      log.info("User '{}' is locked. Please contact your administrator.", username);
      return null;
    }

    Subject subject = resolveSubject();
    login(username, password, host, subject);

    if (newUser) {
      // TODO: Revisit (will save all script kiddies username attempts)?
      userService.saveUser(user);
    }

    if (subject.isAuthenticated()) {
      if (newUser == false) {
        userService.resetUser(user);
      }

      log.info("User '{}' logged in successfully.", subject.getPrincipal());
      return subject;
    } else {
      removeSubject();
      userService.reprimandUser(user);

      log.info(user.isLocked() ?
          "User {} was locked after too many failed attempts" :
          "User {} was reprimanded after a failed attempt", username);

      return null;
    }
  }

  @SneakyThrows
  private Subject resolveSubject() {
    try {
      return SecurityUtils.getSubject();
    } catch (UnavailableSecurityManagerException e) {
      log.error("Failure to get the current Subject:", e);
      throw e;
    }
  }

  private void login(final String username, final char[] password, final String host, Subject subject) {
    // Build token from credentials
    val token = new UsernamePasswordToken(username, password, false, host);

    try {
      // Attempt to login user
      subject.login(token);
    } catch (UnknownAccountException uae) {
      removeSubject();
      log.info("There is no user with username of '{}'", token.getPrincipal());
    } catch (IncorrectCredentialsException ice) {
      removeSubject();
      log.info("Password for user '{}' was incorrect!", token.getPrincipal());
    } catch (LockedAccountException lae) { // TODO: look into this rather than using above hack?
      removeSubject();
      log.info("The account for user '{}' is locked. Please contact your administrator to unlock it.",
          token.getPrincipal());
    } catch (AuthenticationException ae) {
      // FIXME: it seems invalid credentials actually result in:
      // org.apache.shiro.authc.AuthenticationException: Authentication token of
      // type [class org.apache.shiro.authc.UsernamePasswordToken] could not be
      // authenticated by any configured realms. Please ensure that at least one
      // realm can authenticate these tokens. (not IncorrectCredentialsException)
      removeSubject();
      log.error("Unknown error logging in user '{}'. Please contact your administrator.", token.getPrincipal());
    }
  }

  @Override
  public Subject getSubject() {
    return SecurityUtils.getSubject();
  }

  @Override
  public void removeSubject() {
    ThreadContext.remove();
  }

}
