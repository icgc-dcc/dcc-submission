package org.icgc.dcc.web;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.icgc.dcc.model.QProject;

import com.google.code.morphia.Morphia;
import com.google.inject.Inject;

@Path("projects")
public class ProjectResource {

  @Inject
  private Morphia morphia;

  @GET
  @Path("{accessionId}")
  public Response getIt(@PathParam("accessionId") String accessionId) {
    QProject.project.accessionId.eq(accessionId);
    return null;
  }
}
