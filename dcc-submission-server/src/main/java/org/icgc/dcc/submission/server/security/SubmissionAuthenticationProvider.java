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
package org.icgc.dcc.submission.server.security;

import static org.icgc.dcc.submission.core.security.Authorizations.getUsername;

import org.icgc.dcc.submission.core.model.User;
import org.icgc.dcc.submission.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubmissionAuthenticationProvider extends DaoAuthenticationProvider {

  @NonNull
  private final UserService userService;

  @Autowired
  public SubmissionAuthenticationProvider(UserDetailsService userDetailsService, UserService userService) {
    setUserDetailsService(userDetailsService);
    this.userService = userService;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    val username = getUsername(authentication);
    User user = getUser(username);

    // Ensure not locked
    if (user.isLocked()) {
      // Trigger 403
      val message = "User '" + username + "' is locked. Please contact your administrator.";
      log.warn(message);
      throw new LockedException(message);
    }

    try {
      // Check password
      val auth = super.authenticate(authentication);

      // If correct password, reset the user_attempts
      userService.resetUser(user);

      return auth;
    } catch (BadCredentialsException e) {
      // Invalid login, update attempts
      user = userService.reprimandUser(user);

      log.info(
          user.isLocked() ? "User {} was locked after too many failed attempts" : "User {} was reprimanded after a failed attempt",
          username);

      // Let this trigger a 401
      throw e;
    }

  }

  private User getUser(String username) {
    val user = userService.getUserByUsername(username);
    if (!user.isPresent()) {
      throw new UsernameNotFoundException("User with username '" + username + "' not found");
    }

    return user.get();
  }

}
