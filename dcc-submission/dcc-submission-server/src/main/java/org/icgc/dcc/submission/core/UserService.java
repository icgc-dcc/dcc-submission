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
package org.icgc.dcc.submission.core;

import org.icgc.dcc.submission.core.model.QUser;
import org.icgc.dcc.submission.core.model.User;
import org.icgc.dcc.submission.core.morphia.BaseMorphiaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.common.base.Optional;
import com.google.inject.Inject;

/**
 * 
 */
public class UserService extends BaseMorphiaService<User> {

  private static final Logger log = LoggerFactory.getLogger(UserService.class);

  @Inject
  public UserService(Morphia morphia, Datastore datastore) {
    super(morphia, datastore, QUser.user);
    registerModelClasses(User.class);
  }

  public Optional<User> getUserByUsername(String username) {
    User user = this.where(QUser.user.username.eq(username)).singleResult();
    return user == null ? Optional.<User> absent() : Optional.of(user);
  }

  /**
   * Saves the information pertaining to user that is relevant to the locking of users after too many failed attempts.
   * NOT intended for saving roles/permissions and emails at the moment.
   */
  public User saveUser(User user) {
    log.info("saving user {}", user);
    datastore().save(user);
    return user;
  }

  public User reprimandUser(User user) {
    user.incrementAttempts();
    return updateUser(user);
  }

  public User resetUser(User user) {
    user.resetAttempts();
    return updateUser(user);
  }

  /**
   * Only updates the "failedAttempts" for now.
   */
  private User updateUser(User user) {
    log.info("updating user {}", user);
    return datastore().findAndModify( // this will ignore the optimistic lock (@Version)
        datastore().createQuery(User.class).filter("username", user.getUsername()), //
        datastore().createUpdateOperations(User.class).set("failedAttempts", user.getFailedAttempts()), false, false);
  }
}
