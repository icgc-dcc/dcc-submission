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
import java.util.Set;

import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.text.IniRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.web.Authorizations;

import com.google.common.collect.Sets;

/**
 * Accesses user authentication and role authorizations from the realm.ini file.
 */
public class DccWrappingRealm extends IniRealm {

  private final ProjectService projects;

  public DccWrappingRealm(ProjectService projects) {
    this.projects = projects;
  }

  /**
   * TODO: <code>{@link DccRealm#getPermissions(String)}</code>
   */
  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    Collection<String> iniPermissions = super.getAuthorizationInfo(principals).getStringPermissions();
    SimpleAuthorizationInfo sai = new SimpleAuthorizationInfo(Sets.newHashSet(iniPermissions));
    String username = Authorizations.getUsername(principals.getPrimaryPrincipal());
    sai.addStringPermissions(buildProjectPermissions(username));
    return sai;
  }

  private Set<String> buildProjectPermissions(String username) {
    Set<String> stringPermissions = Sets.newLinkedHashSet();
    for(Project project : projects.getProjects()) {
      if(project.hasUser(username)) {
        stringPermissions.add(AuthorizationPrivileges.projectViewPrivilege(project.getKey()));
      }
    }
    return stringPermissions;
  }
}
