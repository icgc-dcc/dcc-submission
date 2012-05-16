package org.icgc.dcc.web;

import com.google.inject.Inject;
import com.typesafe.config.Config;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

@Path("myresource")
public class MyResource {

  // Injected by Guice
  @Inject
  private Config config;

  // Injected by Jersey
  @Context
  private UriInfo uriInfo;

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String getIt() {
    return "Got it! " + uriInfo.getAbsolutePath() + " " + config.getString("http.port");
  }
}
