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
package org.icgc.dcc.http.jersey;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.apache.shiro.codec.Base64;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;
import org.icgc.dcc.shiro.ShiroSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Authentication filter
 * 
 * try it with: $ curl -v -H "Authorization: Basic $(echo -n "brett:brettspasswd" | base64)"
 * http://localhost:5379/ws/myresource
 */
@Provider
@BindingPriority(BindingPriority.AUTHENTICATION)
public class BasicHttpAuthenticationRequestFilter implements ContainerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(BasicHttpAuthenticationRequestFilter.class);

  private static final String HTTP_AUTH_PREFIX = "X-DCC-Auth";

  private static final String TOKEN_INFO_SEPARATOR = ":";

  private static final String WWW_AUTHENTICATE_REALM = "DCC";

  private final UsernamePasswordAuthenticator passwordAuthenticator;

  /**
   * TODO: Change to use annotation (see DCC-818)
   */
  private static final List<String> OPEN_ACCESS_PATHS = // do not provide trailing "/"
      Lists.newArrayList( //
          "/nextRelease/dictionary", //
          "/codeLists" //
      );

  @Inject
  public BasicHttpAuthenticationRequestFilter(UsernamePasswordAuthenticator passwordAuthenticator) {
    checkArgument(passwordAuthenticator != null);
    this.passwordAuthenticator = checkNotNull(passwordAuthenticator);
  }

  @Override
  public void filter(ContainerRequestContext context) throws IOException {

    Optional<String> openAccessPath = getOpenAccessPath(context);
    if(openAccessPath.isPresent()) {
      log.debug("Skipping basic authentication for whitelisted path {}", openAccessPath.get());
      return;
    }

    MultivaluedMap<String, String> headers = context.getHeaders();
    String authorizationHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
    log.debug("authorizationHeader = " + authorizationHeader);
    if(authorizationHeader == null || authorizationHeader.isEmpty()) {
      Response response = createUnauthorizedResponse();
      context.abortWith(response);
    } else {
      // expected to be like: "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
      String[] split = authorizationHeader.split(" ", 2);
      if(split.length != 2 || !split[0].equals(HTTP_AUTH_PREFIX)) {
        log.error("Invalid authorization header: " + authorizationHeader);
        Response response = Response.status(Response.Status.BAD_REQUEST).build();
        context.abortWith(response);
      } else {
        String authenticationToken = split[1];

        String decodedAuthenticationToken = Base64.decodeToString(authenticationToken);

        String[] decoded = decodedAuthenticationToken.split(TOKEN_INFO_SEPARATOR, 2);

        if(decoded.length != 2) {
          log.error("Expected 2 components of decoded authorization header; received " + decoded.length);
          Response response = Response.status(Response.Status.BAD_REQUEST).build();
          context.abortWith(response);
        } else {
          String username = decoded[0];
          log.info("username = \"" + username + "\"");

          String password = decoded[1];
          log.info("password decoded (" + password.length() + " characters long)");

          Subject currentUser = this.passwordAuthenticator.authenticate(username, password.toCharArray(), "");
          if(currentUser == null) {
            Response response = createUnauthorizedResponse();
            context.abortWith(response);
          }
          context.setSecurityContext(new ShiroSecurityContext(currentUser, context.getSecurityContext().isSecure()));
        }
      }
    }

  }

  /**
   * Temporary: see DCC-818
   */
  private Optional<String> getOpenAccessPath(ContainerRequestContext context) {
    UriInfo uriInfo = context.getUriInfo();
    String path = uriInfo.getPath().replaceAll("/$", "");
    return OPEN_ACCESS_PATHS.contains(path) ? Optional.of(path) : Optional.<String> absent();
  }

  private Response createUnauthorizedResponse() {
    return Response
        .status(Response.Status.UNAUTHORIZED)
        .header(HttpHeaders.WWW_AUTHENTICATE,
            String.format("%s realm=\"%s\"", HTTP_AUTH_PREFIX, WWW_AUTHENTICATE_REALM)).build();
  }
}
