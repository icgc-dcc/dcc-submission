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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.UserService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.User;

import com.google.common.collect.Lists;

public class DccDbRealm extends AuthorizingRealm implements DccRealm {

  private final UserService users;

  private final ProjectService projects;

  public DccDbRealm(UserService users, ProjectService projects) {
    this.users = users;
    this.projects = projects;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    String username = (String) principals.getPrimaryPrincipal();
    User user = users.getUser(username);
    SimpleAuthorizationInfo sai = new SimpleAuthorizationInfo(new HashSet<String>(user.getRoles()));
    Set<String> stringPermissions = new HashSet<String>();
    for(Project project : projects.getProjects()) {
      if(project.hasUser(user.getName()) || CollectionUtils.containsAny(project.getGroups(), user.getRoles())) {
        stringPermissions.add(AuthorizationPrivileges.projectViewPrivilege(project.getKey()));
      }
    }
    sai.addStringPermissions(stringPermissions);

    return sai;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    // Authorization is currently only performed via the Shiro INI file, so this is not populated
    return null;
  }

  @Override
  public boolean supports(AuthenticationToken token) {
    return false;
  }

  @Override
  public Collection<String> getRoles(String username) {
    User user = users.getUser(username);
    if(user == null) {
      return Lists.newArrayList();
    }
    return user.getRoles();
  }

}
