package org.icgc.dcc.http.jersey;

import java.io.IOException;

import javax.ws.rs.BindingPriority;
import javax.ws.rs.core.RequestHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.FilterContext;
import javax.ws.rs.ext.PreMatchRequestFilter;
import javax.ws.rs.ext.Provider;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.typesafe.config.Config;

/* filters are annotated this way in JAX-RS 2.0 */
@Provider
/* the lower the number, the higher the priority [https://github.com/resteasy/Resteasy/pull/26/files - TODO: confirm] */
@BindingPriority(0)
/* we want it bound to all resources (global binding is default) */
/**
 * Authentication filter
 * 
 * @author Anthony Cros (anthony.cros@oicr.on.ca)
 */
public class BasicHttpAuthenticationRequestFilter implements PreMatchRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(BasicHttpAuthenticationRequestFilter.class);

  private static final String WWW_AUTHENTICATE_REALM = "DCC"; // TODO: put elsewhere, application-wide name

  @Inject
  private Config config;

  @Override
  public void preMatchFilter(FilterContext filterContext) throws IOException {

    RequestHeaders headers = filterContext.getRequest().getHeaders();

    String authorizationHeader = headers.getHeader(HttpHeaders.AUTHORIZATION);
    log.info("authorizationHeader = " + authorizationHeader);

    String authenticateHeader = headers.getHeader(HttpHeaders.WWW_AUTHENTICATE);
    log.info("authenticateHeader = " + authenticateHeader);

    if(authorizationHeader == null || authorizationHeader.isEmpty()) {
      ResponseBuilder responseBuilder = Response.status(Response.Status.UNAUTHORIZED);
      responseBuilder.header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"" + WWW_AUTHENTICATE_REALM + "\"");
      Response response = responseBuilder.build();
      log.info("response = " + response.getStatus() + ", " + response.getHeaders().asMap());

      filterContext.setResponse(response); // does not seem to work for now, probably not implemented in jersey for now
                                           // (TODO)
    } else {
      if(authorizationHeader != null && !authorizationHeader.isEmpty()) {
        Subject currentSubject = SecurityUtils.getSubject();

        Session session = currentSubject.getSession();
        session.setAttribute("someKey", "aValue");
        String value = (String) session.getAttribute("someKey");
        if(value.equals("aValue")) {
          log.info("Retrieved the correct value! [" + value + "]");
        }

        // let's login the current user so we can check against roles and permissions:
        if(!currentSubject.isAuthenticated()) {
          UsernamePasswordToken token = new UsernamePasswordToken("brett", "brettspasswd");
          token.setRememberMe(true);
          try {
            currentSubject.login(token);
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
          log.info("User [" + currentSubject.getPrincipal() + "] logged in successfully.");
        }

      }

      // String authorization = getAuthorizationHeader(request);
      // String sessionId = extractSessionId(request);
      // HttpAuthorizationToken token = new HttpAuthorizationToken(X_OPAL_AUTH, authorization);
    }

  }
}
