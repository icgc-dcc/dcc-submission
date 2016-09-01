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
package org.icgc.dcc.submission.server.service;

import static com.google.common.base.Optional.fromNullable;

import org.icgc.dcc.submission.core.model.User;
import org.icgc.dcc.submission.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Optional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserService {

  @NonNull
  private final UserRepository userRepository;

  public Optional<User> getUserByUsername(String username) {
    return fromNullable(userRepository.findUserByUsername(username));
  }

  /**
   * Saves the information pertaining to user that is relevant to the locking of users after too many failed attempts.
   * NOT intended for saving roles/permissions and emails at the moment.
   */
  public User saveUser(User user) {
    log.info("Saving user {}", user);
    return userRepository.saveUser(user);
  }

  public User reprimandUser(User user) {
    user.incrementAttempts();
    return userRepository.updateUser(user);
  }

  public User resetUser(User user) {
    user.resetAttempts();
    return userRepository.updateUser(user);
  }

}
