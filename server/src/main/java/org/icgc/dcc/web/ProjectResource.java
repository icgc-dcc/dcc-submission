package org.icgc.dcc.web;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Projects;
import org.icgc.dcc.model.QProject;

import com.google.inject.Inject;
import com.mongodb.MongoException.DuplicateKey;

@Path("projects")
public class ProjectResource {

  @Inject
  private Projects projects;

  @GET
  @Produces("application/json")
  public Response getProjects() {
    List<Project> projectlist = projects.query().list();
    if(projectlist == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok(projectlist).build();
  }

  @POST
  public Response addProject(Project project) {
    checkArgument(project != null);
    try {
      projects.datastore().save(project);
      return Response.created(UriBuilder.fromResource(ProjectResource.class).path(project.getAccessionId()).build())
          .build();
    } catch(DuplicateKey e) {
      return Response.status(Status.BAD_REQUEST).build();
    }
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

  @PUT
  @Path("{accessionId}")
  public Response updateProject(@PathParam("accessionId") String accessionId) {
    Project project = projects.where(QProject.project.accessionId.eq(accessionId)).uniqueResult();
    if(project == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok(project).build();
  }

  @GET
  @Path("{accessionId}/releases")
  @Produces("application/json")
  public Response getReleases(@PathParam("accessionId") String accessionId) {
    Project project = projects.where(QProject.project.accessionId.eq(accessionId)).uniqueResult();
    if(project == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok(projects.getReleases(project)).build();
  }
}
