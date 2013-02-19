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
package org.icgc.dcc.web;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.shiro.AuthorizationPrivileges;
import org.icgc.dcc.shiro.ShiroSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils method to bring together logic pertaining to authorization checks.
 */
public class Authorizations {

  private static final Logger log = LoggerFactory.getLogger(Authorizations.class);

  public static String getUsername(Object principal) {
    return principal.toString(); // TODO: there doesn't seem to be another way than using toString...
  }

  static String getUsername(SecurityContext securityContext) {
    Object principal = ((ShiroSecurityContext) securityContext).getSubject().getPrincipal();
    return getUsername(principal);
  }

  static boolean hasPrivilege(SecurityContext securityContext, String privilege) {
    ShiroSecurityContext shiroSecurityContext = (ShiroSecurityContext) securityContext;
    Subject subject = shiroSecurityContext.getSubject();
    log.debug("Checking that subject {} has privilege {}", subject.getPrincipal(), privilege);

    return subject.isPermitted(privilege);
  }

  static boolean hasPrivilege(SecurityContext securityContext, AuthorizationPrivileges privilege) {
    return hasPrivilege(securityContext, privilege.getPrefix());
  }

  static boolean isOmnipotentUser(SecurityContext securityContext) {
    return hasPrivilege(securityContext, AuthorizationPrivileges.ALL.getPrefix());
  }

  static boolean hasReleaseViewPrivilege(SecurityContext securityContext) {
    return hasPrivilege(securityContext, AuthorizationPrivileges.RELEASE_VIEW.getPrefix());
  }

  static boolean hasReleaseClosePrivilege(SecurityContext securityContext) {
    return hasPrivilege(securityContext, AuthorizationPrivileges.RELEASE_CLOSE.getPrefix());
  }

  /**
   * Connected user has access to the projects it owns:
   * <code>{@link DccDbRealm#doGetAuthorizationInfo(PrincipalCollection)}</code>
   */
  static boolean hasSpecificProjectPrivilege(SecurityContext securityContext, String projectKey) {
    return hasPrivilege(securityContext, AuthorizationPrivileges.projectViewPrivilege(projectKey));
  }

  static Response unauthorizedResponse() {
    return unauthorizedResponse(false);
  }

  static Response unauthorizedResponse(boolean important) {
    ServerErrorResponseMessage errorMessage = new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED);
    if(important) {
      log.info("unauthorized action: {}", errorMessage);
    }
    return Response.status(Status.UNAUTHORIZED).entity(errorMessage).build();
  }
}
