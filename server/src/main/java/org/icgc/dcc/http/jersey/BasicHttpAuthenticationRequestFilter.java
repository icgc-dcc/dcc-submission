package org.icgc.dcc.http.jersey;

import java.io.IOException;

import javax.ws.rs.ext.FilterContext;
import javax.ws.rs.ext.PreMatchRequestFilter;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.typesafe.config.Config;

/* filters are annotated this way in JAX-RS 2.0 */
@Provider
// @BindingPriority(SECURITY)//TODO?
public class BasicHttpAuthenticationRequestFilter implements PreMatchRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(BasicHttpAuthenticationRequestFilter.class);

  @Inject
  private Config config;

  @Override
  public void preMatchFilter(FilterContext filterContext) throws IOException {
    log.info("filtering: " + this.config.getString("shiro.realm"));
  }
}
