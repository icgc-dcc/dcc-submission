package org.icgc.dcc.http.jersey;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.shiro.codec.Base64;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;

/**
 * Authentication filter
 * 
 * try it with: $ curl -v -H "Authorization: Basic $(echo -n "brett:brettspasswd" | base64)"
 * http://localhost:5379/ws/myresource
 */
@Provider
@BindingPriority(BindingPriority.SECURITY)
public class BasicHttpAuthenticationRequestFilter implements ContainerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(BasicHttpAuthenticationRequestFilter.class);

  private static final String HTTP_AUTH_PREFIX = "X-DCC-Auth";

  private static final String TOKEN_INFO_SEPARATOR = ":";

  private static final String WWW_AUTHENTICATE_REALM = "DCC";

  private final UsernamePasswordAuthenticator passwordAuthenticator;

  @Inject
  public BasicHttpAuthenticationRequestFilter(UsernamePasswordAuthenticator passwordAuthenticator) {
    checkArgument(passwordAuthenticator != null);
    this.passwordAuthenticator = passwordAuthenticator;
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {

    MultivaluedMap<String, String> headers = containerRequestContext.getHeaders();

    String authorizationHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
    log.debug("authorizationHeader = " + authorizationHeader);

    if(authorizationHeader == null || authorizationHeader.isEmpty()) {
      Response response = createUnauthorizedResponse();
      containerRequestContext.abortWith(response);
    } else {
      // expected to be like: "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
      String[] split = authorizationHeader.split(" ", 2);
      if(split.length != 2 || !split[0].equals(HTTP_AUTH_PREFIX)) {
        log.error("Invalid authorization header: " + authorizationHeader);
        Response response = Response.status(Response.Status.BAD_REQUEST).build();
        containerRequestContext.abortWith(response);
      } else {
        String authenticationToken = split[1];

        String decodedAuthenticationToken = Base64.decodeToString(authenticationToken);

        String[] decoded = decodedAuthenticationToken.split(TOKEN_INFO_SEPARATOR, 2);

        if(decoded.length != 2) {
          log.error("Expected 2 components of decoded authorization header; received " + decoded.length);
          Response response = Response.status(Response.Status.BAD_REQUEST).build();
          containerRequestContext.abortWith(response);
        } else {
          String username = decoded[0];
          log.info("username = \"" + username + "\"");

          String password = decoded[1];
          log.info("password decoded (" + password.length() + " characters long)");

          if(this.passwordAuthenticator.authenticate(username, password.toCharArray(), "") == false) {
            Response response = createUnauthorizedResponse();
            containerRequestContext.abortWith(response);
          }
        }
      }
    }

  }

  private Response createUnauthorizedResponse() {
    return Response
        .status(Response.Status.UNAUTHORIZED)
        .header(HttpHeaders.WWW_AUTHENTICATE,
            String.format("%s realm=\"%s\"", HTTP_AUTH_PREFIX, WWW_AUTHENTICATE_REALM)).build();
  }
}
