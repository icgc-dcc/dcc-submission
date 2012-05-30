package org.icgc.dcc.http.jersey;

import java.io.IOException;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.core.RequestHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.FilterContext;
import javax.ws.rs.ext.PreMatchRequestFilter;
import javax.ws.rs.ext.Provider;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;

/* filters are annotated this way in JAX-RS 2.0 */
@Provider
/* the lower the number, the higher the priority [https://github.com/resteasy/Resteasy/pull/26/files - TODO: confirm] */
@BindingPriority(0)
/* we want it bound to all resources (global binding is default) */
/**
 * Authentication filter
 * 
 * try it with: $ curl -v -H "Authorization: Basic $(echo -n "brett:brettspasswd" | base64)" http://localhost:5379/ws/myresource
 * 
 * @author Anthony Cros (anthony.cros@oicr.on.ca)
 */
public class BasicHttpAuthenticationRequestFilter implements PreMatchRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(BasicHttpAuthenticationRequestFilter.class);

  private static final String HTTP_BASIC = "Basic"; // TODO: existing constant somewhere?

  private static final String HTTP_BASIC_REALM = "realm"; // TODO: existing constant somewhere?

  private static final String TOKEN_INFO_SEPARATOR = ":";

  private static final String WWW_AUTHENTICATE_REALM = "DCC"; // TODO: put elsewhere, application-wide name

  @Override
  public void preMatchFilter(FilterContext filterContext) throws IOException {

    RequestHeaders headers = filterContext.getRequest().getHeaders();

    // get authorization header
    String authorizationHeader = headers.getHeader(HttpHeaders.AUTHORIZATION);
    log.info("authorizationHeader = " + authorizationHeader);

    // check if provided
    if(authorizationHeader == null || authorizationHeader.isEmpty()) {
      Response response = // TODO: prettify
          this.buildErrorResponse(Response.Status.UNAUTHORIZED,//
              HttpHeaders.WWW_AUTHENTICATE, HTTP_BASIC + " " + HTTP_BASIC_REALM + "=" + //
                  "\"" + WWW_AUTHENTICATE_REALM + "\"");

      // does not seem to work for now, probably not implemented in jersey for now (TODO)
      filterContext.setResponse(response);
    } else {

      // expected to be like: "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ=="
      String[] split = authorizationHeader.split(" ", 2);
      if(split.length != 2 || !split[0].equals(HTTP_BASIC)) {
        Response response = this.buildErrorResponse(Response.Status.BAD_REQUEST); // TODO: add error message?
        filterContext.setResponse(response); // see comment above
      } else {

        // grabbing the "QWxhZGRpbjpvcGVuIHNlc2FtZQ==" part
        String authenticationToken = split[1];

        // decoding it from base 64 ()
        String decodeToString = Base64.decodeToString(authenticationToken);

        // splitting it (username and password are expected to be colon-separated)
        String[] decoded = decodeToString.split(TOKEN_INFO_SEPARATOR, 2);// adapted from Obiba's
                                                                         // HttpAuthorizationToken.java

        if(decoded.length != 2) {
          Response response = this.buildErrorResponse(Response.Status.BAD_REQUEST); // TODO: add error message?
          filterContext.setResponse(response); // see comment above
          // TODO: should we allow using "return;"...?
        } else {

          // grab username
          String username = decoded[0];
          log.info("username = \"" + username + "\"");

          // grab password
          String password = decoded[1];
          log.info("password decoded (" + password.length() + " characters long)");

          // grab current user
          Subject currentUser = SecurityUtils.getSubject(); // no need to inject anything

          // build token from credentials
          UsernamePasswordToken token = new UsernamePasswordToken(username, password);
          token.setRememberMe(true);

          // TODO: proper shiro handling (this is dummy)
          try {
            // attempt to login user
            currentUser.login(token);
          } catch(UnknownAccountException uae) {
            log.info("There is no user with username of " + token.getPrincipal());
          } catch(IncorrectCredentialsException ice) {
            log.info("Password for account " + token.getPrincipal() + " was incorrect!");
          } catch(LockedAccountException lae) {
            log.info("The account for username " + token.getPrincipal() + " is locked.  "
                + "Please contact your administrator to unlock it.");
          }
          // ... catch more exceptions here (maybe custom ones specific to your application?
          catch(AuthenticationException ae) {
            // unexpected condition? error?
          }

          // say who they are:
          // print their identifying principal (in this case, a username):
          log.info("User [" + currentUser.getPrincipal() + "] logged in successfully.");
        }
      }
    }

  }

  private Response buildErrorResponse(Status status) {
    return this.buildErrorResponse(status, null, null);
  }

  private Response buildErrorResponse(Status status, String scheme, String value) { // TODO: should use map (or at least
                                                                                    // entry) for headers
    ResponseBuilder responseBuilder = Response.status(status);
    if(null != scheme) {
      responseBuilder.header(scheme, value);
    }

    Response response = responseBuilder.build();
    log.info("response = " + response.getStatus() + ", " + response.getHeaders().asMap());

    return response;
  }
}
