package org.icgc.dcc.http.jersey;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.core.RequestHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.FilterContext;
import javax.ws.rs.ext.PreMatchRequestFilter;
import javax.ws.rs.ext.Provider;

import org.apache.shiro.codec.Base64;
import org.apache.shiro.mgt.SecurityManager;
import org.icgc.dcc.shiro.ShiroPasswordAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;

/**
 * Authentication filter
 * 
 * try it with: $ curl -v -H "Authorization: Basic $(echo -n "brett:brettspasswd" | base64)"
 * http://localhost:5379/ws/myresource
 * 
 * @author Anthony Cros (anthony.cros@oicr.on.ca)
 */
@Provider
@BindingPriority(BindingPriority.SECURITY)
public class BasicHttpAuthenticationRequestFilter implements PreMatchRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(BasicHttpAuthenticationRequestFilter.class);

  private static final String HTTP_BASIC = "Basic"; // TODO: existing constant somewhere?

  private static final String TOKEN_INFO_SEPARATOR = ":";

  private static final String WWW_AUTHENTICATE_REALM = "DCC"; // TODO: put elsewhere, application-wide name

  public SecurityManager securityManager;

  @Inject
  public BasicHttpAuthenticationRequestFilter(SecurityManager securityManager) {
    checkArgument(securityManager != null);
    this.securityManager = securityManager;
  }

  @Override
  public void preMatchFilter(FilterContext filterContext) throws IOException {

    RequestHeaders headers = filterContext.getRequest().getHeaders();

    // get authorization header
    String authorizationHeader = headers.getHeader(HttpHeaders.AUTHORIZATION);
    log.debug("authorizationHeader = " + authorizationHeader);

    // check if provided
    if(authorizationHeader == null || authorizationHeader.isEmpty()) {
      Response response =
          filterContext
              .createResponse()
              .status(Response.Status.UNAUTHORIZED)
              .header(HttpHeaders.WWW_AUTHENTICATE,
                  String.format("%s realm=\"%s\"", HTTP_BASIC, WWW_AUTHENTICATE_REALM)).build();

      // TODO: does not seem to work for now, probably not implemented in jersey for now (m04)
      filterContext.setResponse(response);
    } else {
      // expected to be like: "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
      String[] split = authorizationHeader.split(" ", 2);
      if(split.length != 2 || !split[0].equals(HTTP_BASIC)) {
        // TODO: add error message?
        Response response = filterContext.createResponse().status(Response.Status.BAD_REQUEST).build();
        filterContext.setResponse(response);
      } else {

        // grabbing the "QWxhZGRpbjpvcGVuIHNlc2FtZQ==" part
        String authenticationToken = split[1];

        // decoding it from base 64 ()
        String decodedAuthenticationToken = Base64.decodeToString(authenticationToken);

        // splitting it (username and password are expected to be colon-separated)
        String[] decoded = decodedAuthenticationToken.split(TOKEN_INFO_SEPARATOR, 2);// adapted from Obiba's
        // HttpAuthorizationToken.java

        if(decoded.length != 2) {
          // TODO: add error message?
          Response response = filterContext.createResponse().status(Response.Status.BAD_REQUEST).build();
          filterContext.setResponse(response); // see comment above
          // TODO: should we allow using "return;"...?
        } else {

          // grab username
          String username = decoded[0];
          log.info("username = \"" + username + "\"");

          // grab password
          String password = decoded[1];
          log.info("password decoded (" + password.length() + " characters long)");

          // The empty string here is for the host; this can be added later for host filtering and/or logging
          new ShiroPasswordAuthenticator(securityManager).authenticate(username, password, "");
        }
      }
    }

  }
}
