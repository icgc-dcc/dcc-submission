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
package org.icgc.dcc.submission.http.jersey;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.WWW_AUTHENTICATE;
import static javax.ws.rs.BindingPriority.AUTHENTICATION;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.apache.commons.lang.StringUtils.isBlank;

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
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

/**
 * Basic HTTTP authentication filter that binds Shiro's {@link Subject} to the current thread for the life cycle of the
 * request.
 * <p>
 * Example client usage:
 * <code>$ curl -v -H "Authorization: Basic $(echo -n "brett:brettspasswd" | base64)" http://localhost:5379/ws/myresource</code>
 */
@Slf4j
@Provider
@BindingPriority(AUTHENTICATION)
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class BasicHttpAuthenticationFilter implements ContainerRequestFilter, ContainerResponseFilter {

  /**
   * Custom authentication headers.
   */
  public static final String DEFAULT_AUTH_HOST = "";
  public static final String HTTP_AUTH_PREFIX = "X-DCC-Auth";
  public static final String TOKEN_INFO_SEPARATOR = ":";
  public static final String WWW_AUTHENTICATE_REALM = "DCC";

  /**
   * List of paths that are publicly accessible.
   * 
   * TODO: Change to use annotation (see DCC-818).
   * <p>
   * Note: do not provide trailing "/" in paths here.
   */
  private static final List<String> OPEN_ACCESS_PATHS = ImmutableList.of(
      "/nextRelease/dictionary",
      "/codeLists",
      "/dictionaries",
      "/executiveReports/projectDataType",
      "/executiveReports/projectSequencingStrategy"
      );

  /**
   * Request property that carries the previous thread name within the request for later restoration.
   */
  private static final String THREAD_NAME_PROPERTY = BasicHttpAuthenticationFilter.class.getName() + ".thread.name";

  /**
   * Delegate that performs the actual authentication.
   */
  @NonNull
  private final UsernamePasswordAuthenticator authenticator;

  @Override
  public void filter(ContainerRequestContext context) {
    val openAccessPath = getOpenAccessPath(context);
    if (openAccessPath.isPresent()) {
      log.debug("Skipping basic authentication for whitelisted path '{}'", openAccessPath.get());
      return;
    }

    // Extract the authorization header
    val headers = context.getHeaders();
    val authorizationHeader = headers.getFirst(AUTHORIZATION);
    log.debug("authorizationHeader:  '{}'", authorizationHeader);
    if (isBlank(authorizationHeader)) {
      abort(context);
      return;
    }

    // Expected to be of the form: "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
    String[] parts = authorizationHeader.split(" ", 2);
    if (parts.length != 2 || !parts[0].equals(HTTP_AUTH_PREFIX)) {
      abort(context, authorizationHeader);
      return;
    }

    // Decode the Base64-concatenated token
    val authenticationToken = parts[1];
    val decodedAuthenticationToken = Base64.decodeToString(authenticationToken);
    String[] decoded = decodedAuthenticationToken.split(TOKEN_INFO_SEPARATOR, 2);
    if (decoded.length != 2) {
      abort(context, decoded.length);
      return;
    }

    // Authenticate the parsed user's credentials
    val username = decoded[0];
    val password = decoded[1];
    authenticate(context, username, password);
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    // Remove subject bound to the current thread
    authenticator.removeSubject();

    // Be nice by restoring to the original "Grizzyly(1)" style thread name
    restoreThreadName(requestContext);
  }

  private void authenticate(ContainerRequestContext context, String username, String password) {
    // Use "username-1" as thread name for improved logging
    updateThreadName(context, username);

    // Raison d'être
    val currentUser = authenticator.authenticate(username, password.toCharArray(), DEFAULT_AUTH_HOST);
    if (currentUser == null) {
      abort(context);
    }

    // Allow JAX-RS access to Shiro
    val https = context.getSecurityContext().isSecure();
    val securityContext = new ShiroSecurityContext(currentUser, https);
    context.setSecurityContext(securityContext);
  }

  private static void abort(ContainerRequestContext context) {
    val response = createUnauthorizedResponse();
    context.abortWith(response);
  }

  private static void abort(ContainerRequestContext context, String authorizationHeader) {
    log.error("Invalid authorization header: '{}'", authorizationHeader);
    val response = Response.status(BAD_REQUEST).build();
    context.abortWith(response);
  }

  private static void abort(ContainerRequestContext context, int decodedLength) {
    log.error("Expected 2 components of decoded authorization header; received {}", decodedLength);
    val response = Response.status(BAD_REQUEST).build();
    context.abortWith(response);
  }

  private static Response createUnauthorizedResponse() {
    return Response
        .status(UNAUTHORIZED)
        .header(WWW_AUTHENTICATE,
            String.format("%s realm=\"%s\"", HTTP_AUTH_PREFIX, WWW_AUTHENTICATE_REALM)).build();
  }

  /**
   * Temporary: see DCC-818
   */
  private static Optional<String> getOpenAccessPath(ContainerRequestContext context) {
    String path = removePathTrailingSlash(context.getUriInfo());
    return isGetMethod(context) && isOpenPath(path) ?
        Optional.of(path) : Optional.<String> absent();
  }

  private static boolean isGetMethod(ContainerRequestContext context) {
    return Method.GET.getMethodString().equals(context.getMethod());
  }

  private static boolean isOpenPath(String path) {
    for (val openPath : OPEN_ACCESS_PATHS) {
      if (path.startsWith(openPath)) {
        return true;
      }
    }

    return false;
  }

  private static String removePathTrailingSlash(UriInfo uriInfo) {
    return uriInfo.getPath().replaceAll("/$", DEFAULT_AUTH_HOST);
  }

  private static void updateThreadName(ContainerRequestContext context, String username) {
    // Remember current thread name
    val currentThreadName = Thread.currentThread().getName();
    context.setProperty(THREAD_NAME_PROPERTY, currentThreadName);

    val threadNumber = getThreadNumber(currentThreadName);
    val newThreadName = username + "-" + threadNumber;

    // Name the thread after the user
    Thread.currentThread().setName(newThreadName);
  }

  private static String getThreadNumber(String threadName) {
    try {
      return threadName.substring(threadName.indexOf('(') + 1, threadName.indexOf(')')).trim();
    } catch (Exception e) {
      // Best effort
      return "?";
    }
  }

  private static void restoreThreadName(ContainerRequestContext requestContext) {
    val threadName = (String) requestContext.getProperty(THREAD_NAME_PROPERTY);
    if (threadName != null) {
      // Reinstate old thread name
      Thread.currentThread().setName(threadName);
    }
  }

}
