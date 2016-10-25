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
package org.icgc.dcc.submission.core.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.icgc.dcc.submission.core.util.Constants;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Utils method to bring together logic pertaining to authorization checks.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Authorizations {

  public static final String ADMIN_ROLE = Constants.Authorizations_ADMIN_ROLE; // TODO: hardcoded value..!! (DCC-759)

  private static final String SUPERUSER_AUTHORITY = "*";

  public static String getUsername(Authentication authentication) {
    if (authentication == null) return null;

    return authentication.getName();
  }

  public static boolean isAdmin(Authentication authentication) {
    return hasRole(authentication, Authorizations.ADMIN_ROLE);
  }

  public static boolean isSuperUser(Authentication authentication) {
    return hasAuthority(authentication, Authority.ALL.getPrefix());
  }

  public static boolean hasReleaseViewAuthority(Authentication authentication) {
    return hasAuthority(authentication, Authority.RELEASE_VIEW.getPrefix());
  }

  public static boolean hasReleaseCloseAuthority(Authentication authentication) {
    return hasAuthority(authentication, Authority.RELEASE_CLOSE.getPrefix());
  }

  public static boolean hasSubmissionSignoffAuthority(Authentication authentication) {
    return hasAuthority(authentication, Authority.SUBMISSION_SIGNOFF.getPrefix());
  }

  /**
   * Currently unused.
   */
  public static boolean hasReleaseModifyAuthority(Authentication authentication) {
    return hasAuthority(authentication, Authority.RELEASE_MODIFY.getPrefix());
  }

  /**
   * Connected user has access to the projects it owns:
   * <code>{@link DccDbRealm#doGetAuthorizationInfo(PrincipalCollection)}</code>
   */
  public static boolean hasSpecificProjectPrivilege(Authentication authentication, String projectKey) {
    return hasAuthority(authentication, Authority.projectViewPrivilege(projectKey));
  }

  private static boolean hasAuthority(Authentication authentication, String authority) {
    if (authentication == null) return false;

    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(a -> a.equals(authority) || a.equals(SUPERUSER_AUTHORITY));
  }

  private static boolean hasRole(Authentication authentication, String role) {
    return hasAuthority(authentication, "ROLE_" + role.toUpperCase());
  }

}
