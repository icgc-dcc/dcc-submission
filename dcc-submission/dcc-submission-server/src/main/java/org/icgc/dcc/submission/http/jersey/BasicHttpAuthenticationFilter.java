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
package org.icgc.dcc.submission.http.jersey;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.BindingPriority.AUTHENTICATION;

import java.util.List;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.shiro.codec.Base64;
import org.glassfish.grizzly.http.Method;
import org.icgc.dcc.submission.security.UsernamePasswordAuthenticator;
import org.icgc.dcc.submission.shiro.ShiroSecurityContext;

import com.google.common.base.Optional;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;

/**
 * Authentication filter
 * 
 * try it with: $ curl -v -H "Authorization: Basic $(echo -n "brett:brettspasswd" | base64)"
 * http://localhost:5379/ws/myresource
 */
@Provider
@BindingPriority(AUTHENTICATION)
@Slf4j
@RequiredArgsConstructor(onConstructor = @_(@Inject))
public class BasicHttpAuthenticationFilter implements ContainerRequestFilter, ContainerResponseFilter {

  /**
   * Custome authentication headers.
   */
  private static final String HTTP_AUTH_PREFIX = "X-DCC-Auth";
  private static final String TOKEN_INFO_SEPARATOR = ":";
  private static final String WWW_AUTHENTICATE_REALM = "DCC";

  /**
   * Delegate that performs the actual authentication.
   */
  @NonNull
  private final UsernamePasswordAuthenticator authenticator;

  /**
   * List of paths that are publicly accessible.
   * 
   * TODO: Change to use annotation (see DCC-818).
   * <p>
   * Note: do not provide trailing "/" in paths here.
   */
  private static final List<String> OPEN_ACCESS_PATHS =
      newArrayList(
          "/nextRelease/dictionary",
          "/codeLists",
          "/dictionaries"
      );

  @Override
  public void filter(ContainerRequestContext context) {
    val openAccessPath = getOpenAccessPath(context);
    if (openAccessPath.isPresent()) {
      log.debug("Skipping basic authentication for whitelisted path {}", openAccessPath.get());
      return;
    }

    val headers = context.getHeaders();
    val authorizationHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
    log.debug("authorizationHeader:  '{}'", authorizationHeader);
    if (authorizationHeader == null || authorizationHeader.isEmpty()) {
      abort(context);
    } else {
      // expected to be like: "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
      val split = authorizationHeader.split(" ", 2);
      if (split.length != 2 || !split[0].equals(HTTP_AUTH_PREFIX)) {
        abort(context, authorizationHeader);
      } else {
        val authenticationToken = split[1];
        val decodedAuthenticationToken = Base64.decodeToString(authenticationToken);
        val decoded = decodedAuthenticationToken.split(TOKEN_INFO_SEPARATOR, 2);
        if (decoded.length != 2) {
          abort(context, decoded.length);
        } else {
          authenticate(context, decoded[0], decoded[1]);
        }
      }
    }
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    // Remove subject bound to the current thread
    authenticator.removeSubject();
  }

  private void authenticate(ContainerRequestContext context, String username, String password) {
    log.info("username = '{}'", username);
    log.info("password decoded ({} characters long)", password.length());

    val currentUser = authenticator.authenticate(username, password.toCharArray(), "");
    if (currentUser == null) {
      abort(context);
    }

    context.setSecurityContext(new ShiroSecurityContext(currentUser, context.getSecurityContext().isSecure()));
  }

  private void abort(ContainerRequestContext context) {
    val response = createUnauthorizedResponse();
    context.abortWith(response);
  }

  private void abort(ContainerRequestContext context, String authorizationHeader) {
    log.error("Invalid authorization header: " + authorizationHeader);
    val response = Response.status(Response.Status.BAD_REQUEST).build();
    context.abortWith(response);
  }

  private void abort(ContainerRequestContext context, int decodedLength) {
    log.error("Expected 2 components of decoded authorization header; received " + decodedLength);
    val response = Response.status(Response.Status.BAD_REQUEST).build();
    context.abortWith(response);
  }

  private Response createUnauthorizedResponse() {
    return Response
        .status(Response.Status.UNAUTHORIZED)
        .header(HttpHeaders.WWW_AUTHENTICATE,
            String.format("%s realm=\"%s\"", HTTP_AUTH_PREFIX, WWW_AUTHENTICATE_REALM)).build();
  }

  /**
   * Temporary: see DCC-818
   */
  private Optional<String> getOpenAccessPath(ContainerRequestContext context) {
    String path = removePathTrailingSlash(context.getUriInfo());
    return isGetMethod(context) && isOpenPath(path) ?
        Optional.of(path) : Optional.<String> absent();
  }

  private boolean isGetMethod(ContainerRequestContext context) {
    return Method.GET.getMethodString().equals(context.getMethod());
  }

  private boolean isOpenPath(String path) {
    for (val openPath : OPEN_ACCESS_PATHS) {
      if (path.startsWith(openPath)) {
        return true;
      }
    }

    return false;
  }

  private String removePathTrailingSlash(UriInfo uriInfo) {
    return uriInfo.getPath().replaceAll("/$", "");
  }

}
