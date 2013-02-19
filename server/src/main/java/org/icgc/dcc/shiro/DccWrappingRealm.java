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
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.text.IniRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.web.Authorizations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Accesses user authentication and role authorizations from the realm.ini file.
 */
public class DccWrappingRealm extends IniRealm {

  private static final Logger log = LoggerFactory.getLogger(DccWrappingRealm.class);

  private final ProjectService projects;

  public DccWrappingRealm(ProjectService projects) {
    this.projects = projects;

    System.out.println(">>");

  }

  /**
   * TODO: <code>{@link DccRealm#getPermissions(String)}</code>
   */
  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    String username = Authorizations.getUsername(principals.getPrimaryPrincipal());
    log.debug("Putting together authorizations for user {}", username);

    SimpleAccount simpleAccount = super.getUser(username);
    Collection<String> iniRoles = simpleAccount.getRoles();
    Set<String> projectSpecificPermissions = buildProjectSpecificPermissions(username, simpleAccount.getRoles());
    log.debug("Ini roles for user {}: {}", username, iniRoles);
    log.debug("Project-specific permissions for user {} (dynamically added): {}", username, projectSpecificPermissions);

    SimpleAuthorizationInfo sai = new SimpleAuthorizationInfo(Sets.newHashSet(iniRoles));
    sai.addStringPermissions(projectSpecificPermissions);

    return sai;
  }

  private Set<String> buildProjectSpecificPermissions(String username, Collection<String> roles) {
    Set<String> permissions = Sets.newLinkedHashSet();
    for(Project project : projects.getProjects()) {
      List<String> groups = Lists.newArrayList(project.getGroups());
      groups.add("admin"); // FIXME?
      if(project.hasUser(username) || CollectionUtils.containsAny(groups, roles)) {
        permissions.add(AuthorizationPrivileges.projectViewPrivilege(project.getKey()));
      }
    }
    return permissions;
  }
}
