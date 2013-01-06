package org.icgc.dcc.bundles;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.jaxrs.JaxrsApiReader;
import com.wordnik.swagger.jaxrs.listing.ApiListing;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

public class SwaggerBundle extends AssetsBundle {

	private static final String EMPTY_FORMAT = "";
	private static final String RESOURCE_PATH = "/swagger-ui/";
	private static final String URI_PATH = "/docs/";

	public SwaggerBundle() {
		super(RESOURCE_PATH, URI_PATH, "index.html");
	}

	@Override
	public void initialize(Bootstrap<?> bootstrap) {
		super.initialize(bootstrap);

		// Set the Swagger suffix to an empty string before Swagger warms up
		JaxrsApiReader.setFormatString(EMPTY_FORMAT);
	}

	@Override
	public void run(Environment environment) {
		super.run(environment);

		environment.addResource(new ApiListingResourceJSON());
	}

	/**
	 * Required to remove the Swagger .{format} extension.
	 * 
	 * @author btiernay
	 */
	@Path("/api-docs")
	@Api("/api-docs")
	@Produces("application/json")
	private static class ApiListingResourceJSON extends ApiListing {
	}

}
