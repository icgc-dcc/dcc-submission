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

package org.icgc.dcc.submission.server.repository;

import static org.icgc.dcc.submission.core.model.QUser.user;

import java.util.List;

import org.icgc.dcc.submission.core.model.QUser;
import org.icgc.dcc.submission.core.model.User;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.NonNull;

public class UserRepository extends AbstractRepository<User, QUser> {

  @Autowired
  public UserRepository(@NonNull Morphia morphia, @NonNull Datastore datastore) {
    super(morphia, datastore, user);
  }

  public List<User> findUsers() {
    return list();
  }

  public User findUserByUsername(@NonNull String username) {
    return uniqueResult(entity.username.eq(username));
  }

  public User saveUser(@NonNull User user) {
    save(user);

    return user;
  }

  public User updateUser(@NonNull User user) {
    return findAndModify(
        createQuery()
            .filter("username", user.getUsername()),
        createUpdateOperations()
            .set("failedAttempts", user.getFailedAttempts()),
        false, false);
  }

}
