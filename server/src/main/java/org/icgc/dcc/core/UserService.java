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
package org.icgc.dcc.core;

import org.icgc.dcc.core.model.QUser;
import org.icgc.dcc.core.model.User;
import org.icgc.dcc.core.morphia.BaseMorphiaService;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.inject.Inject;

/**
 * 
 */
public class UserService extends BaseMorphiaService<User> {

  @Inject
  public UserService(Morphia morphia, Datastore datastore) {
    super(morphia, datastore, QUser.user);
    registerModelClasses(User.class);
  }

  public User getUser(String name) {
    User user = this.where(QUser.user.username.eq(name)).singleResult();
    if(user == null) {
      user = new User();
      user.setUsername(name);
      saveUser(user);
    }
    return user;
  }

  public String getUserEmail(String name) {
    return this.getUser(name).getEmail();
  }

  public void saveUser(User user) {
    datastore().save(user);
  }
}
