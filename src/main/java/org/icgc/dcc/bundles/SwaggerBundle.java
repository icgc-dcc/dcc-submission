package org.icgc.dcc.bundles;

import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

public class SwaggerBundle extends AssetsBundle {

	public static final String RESOURCE_PATH = "/swagger-ui/";
	public static final String URI_PATH = "/docs/";

	public SwaggerBundle() {
		super(RESOURCE_PATH, URI_PATH, "index.html");
	}

	@Override
	public void initialize(Bootstrap<?> bootstrap) {
		super.initialize(bootstrap);
	}

	@Override
	public void run(Environment environment) {
		super.run(environment);

		environment.addResource(new ApiListingResourceJSON());
	}

}
