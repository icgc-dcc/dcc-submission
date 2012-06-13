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

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.icgc.dcc.http.jersey.BasicHttpAuthenticationRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */

public class ShiroPasswordAuthenticator implements PasswordAuthenticator {

  private final SecurityManager securityManager;

  /**
   * @param securityManager
   */
  public ShiroPasswordAuthenticator(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  private static final Logger log = LoggerFactory.getLogger(BasicHttpAuthenticationRequestFilter.class);

  public boolean authenticate(final String username, final String password, final String host) {
    // build token from credentials
    UsernamePasswordToken token = new UsernamePasswordToken(username, password, false, host);

    // grab current user
    // TODO reproduce: ThreadContext.getSubject(); behavior non-statically
    Subject.Builder subjectBuilder = new Subject.Builder(this.securityManager); // avoid using static
    // SecurityUtils
    Subject currentUser = subjectBuilder.buildSubject();

    // TODO: proper shiro handling (this is dummy)
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
    }

    // say who they are:
    // print their identifying principal (in this case, a username):
    log.info("User [" + currentUser.getPrincipal() + "] logged in successfully.");
    return currentUser.isAuthenticated();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.sshd.server.PasswordAuthenticator#authenticate(java.lang.String, java.lang.String,
   * org.apache.sshd.server.session.ServerSession)
   */
  @Override
  public boolean authenticate(String username, String password, ServerSession session) {
    return authenticate(username, password, session.getIoSession().getRemoteAddress().toString());
  }
}
