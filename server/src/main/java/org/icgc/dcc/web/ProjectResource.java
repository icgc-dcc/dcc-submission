package org.icgc.dcc.web;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Projects;
import org.icgc.dcc.model.QProject;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

@Path("projects")
public class ProjectResource {

  @Inject
  private Projects projects;

  @POST
  public Response addProject(Project project) {
    Preconditions.checkArgument(project != null);
    projects.datastore().save(project);
    return Response.created(UriBuilder.fromResource(ProjectResource.class).path(project.accessionId).build()).build();
  }

  @GET
  @Path("{accessionId}")
  public Response getIt(@PathParam("accessionId") String accessionId) {
    Project project = projects.where(QProject.project.accessionId.eq(accessionId)).uniqueResult();
    if(project == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok(project).build();
  }

}
