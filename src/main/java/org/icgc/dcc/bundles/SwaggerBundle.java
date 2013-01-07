package org.icgc.dcc.bundles;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.jaxrs.JaxrsApiReader;
import com.wordnik.swagger.jaxrs.listing.ApiListing;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;

public class SwaggerBundle extends AssetsBundle {

  private static final String EMPTY_FORMAT = "";

  private static final String RESOURCE_PATH = "/swagger-ui/";

  private static final String URI_PATH = "/docs/";

  public SwaggerBundle() {
    super(RESOURCE_PATH, URI_PATH, "index.html");
  }

  @Override
  public final void initialize(Bootstrap<?> bootstrap) {
    super.initialize(bootstrap);

    // Set the Swagger suffix to an empty string before Swagger warms up
    JaxrsApiReader.setFormatString(EMPTY_FORMAT);
  }

  /**
   * Required to remove the Swagger <code>.{format}</code> extension from Swagger resources:
   * <ul>
   * <li><code>/api-docs</code></li>
   * <li><code>/api-docs/{route}</code></li>
   * </ul>
   * 
   * @author btiernay
   */
  @Path("/api-docs")
  @Api("/api-docs")
  @Produces(MediaType.APPLICATION_JSON)
  public static class ApiListingResourceJSON extends ApiListing {
    // Empty
  }

}
