/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.http.jersey.BasicHttpAuthenticationRequestFilter;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * Implements {@code UsernamePasswordAuthenticator} on top of {@code Shiro}
 */
public class ShiroPasswordAuthenticator implements UsernamePasswordAuthenticator {

  @SuppressWarnings("unused")
  private final SecurityManager securityManager;

  @Inject
  public ShiroPasswordAuthenticator(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  private static final Logger log = LoggerFactory.getLogger(BasicHttpAuthenticationRequestFilter.class);

  @Override
  public boolean authenticate(final String username, final char[] password, final String host) {
    // build token from credentials
    UsernamePasswordToken token = new UsernamePasswordToken(username, password, false, host);

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
      log.info("User [" + currentUser.getPrincipal() + "] logged in successfully.");
    }
    return currentUser.isAuthenticated();
  }

}
