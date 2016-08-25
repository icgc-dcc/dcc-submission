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
package org.icgc.dcc.submission.security;

import org.icgc.dcc.submission.core.model.User;
import org.icgc.dcc.submission.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;

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
    val username = authentication.getName();
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
      throw new LockedException(username);
    }

    try {
      val auth = super.authenticate(authentication);

      if (newUser) {
        // TODO: Revisit (will save all script kiddies username attempts)?
        userService.saveUser(user);
      } else {
        // If correct password, reset the user_attempts
        userService.resetUser(user);
      }

      // TODO: Add project privileges here, using ProjectController logic.

      return auth;
    } catch (BadCredentialsException e) {
      // Invalid login, update user_attempts, set attempts+1
      userService.reprimandUser(user);

      log.info(
          user.isLocked() ? "User {} was locked after too many failed attempts" : "User {} was reprimanded after a failed attempt",
          username);

      throw e;
    }

  }

}
