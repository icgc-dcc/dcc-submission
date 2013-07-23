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
package org.icgc.dcc.submission.web.util;

import java.util.Collection;

import javax.ws.rs.core.SecurityContext;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.submission.core.util.Constants;
import org.icgc.dcc.submission.shiro.AuthorizationPrivileges;
import org.icgc.dcc.submission.shiro.ShiroSecurityContext;

/**
 * Utils method to bring together logic pertaining to authorization checks.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Authorizations {

  public static final String ADMIN_ROLE = Constants.Authorizations_ADMIN_ROLE; // TODO: hardcoded value..!! (DCC-759)

  /**
   * Returns the username from a "principal" {@code Object}. Using toString seems to be the only way (as used in Shiro's
   * own classes: {@link SimpleAccountRealm#getUsername()} for instance)
   */
  public static String getUsername(Object principal) {
    return principal == null ? null : principal.toString();
  }

  public static String getUsername(SecurityContext securityContext) { // consider as alternative:
                                                                      // ((ShiroSecurityContext)
    // securityContext).getUserPrincipal().getName();
    Object principal = shiroSecurityContext(securityContext).getSubject().getPrincipal();
    return getUsername(principal);
  }

  /**
   * Casting seems to be the only way (TODO: investigate)
   */
  private static ShiroSecurityContext shiroSecurityContext(SecurityContext securityContext) {
    return (ShiroSecurityContext) securityContext;
  }

  public static Subject getSubject(SecurityContext securityContext) {
    return shiroSecurityContext(securityContext).getSubject();
  }

  public static boolean hasAdminRole(Collection<String> roles) {
    return roles.contains(Authorizations.ADMIN_ROLE);
  }

  public static boolean hasPrivilege(SecurityContext securityContext, String privilege) {
    Subject subject = shiroSecurityContext(securityContext).getSubject();
    log.debug("Checking that subject {} has privilege {}", subject.getPrincipal(), privilege);

    return subject.isPermitted(privilege);
  }

  public static boolean isOmnipotentUser(SecurityContext securityContext) {
    return hasPrivilege(securityContext, AuthorizationPrivileges.ALL.getPrefix());
  }

  public static boolean hasReleaseViewPrivilege(SecurityContext securityContext) {
    return hasPrivilege(securityContext, AuthorizationPrivileges.RELEASE_VIEW.getPrefix());
  }

  public static boolean hasReleaseClosePrivilege(SecurityContext securityContext) {
    return hasPrivilege(securityContext, AuthorizationPrivileges.RELEASE_CLOSE.getPrefix());
  }

  public static boolean hasSubmissionSignoffPrivilege(SecurityContext securityContext) {
    return hasPrivilege(securityContext, AuthorizationPrivileges.SUBMISSION_SIGNOFF.getPrefix());
  }

  public static boolean hasReleaseModifyPrivilege(SecurityContext securityContext) {
    return hasPrivilege(securityContext, AuthorizationPrivileges.RELEASE_MODIFY.getPrefix());
  }

  /**
   * Connected user has access to the projects it owns:
   * <code>{@link DccDbRealm#doGetAuthorizationInfo(PrincipalCollection)}</code>
   */
  public static boolean hasSpecificProjectPrivilege(SecurityContext securityContext, String projectKey) {
    return hasPrivilege(securityContext, AuthorizationPrivileges.projectViewPrivilege(projectKey));
  }

}
