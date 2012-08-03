package org.icgc.dcc.web;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.apache.shiro.SecurityUtils;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.QProject;

import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.inject.Inject;
import com.mongodb.MongoException.DuplicateKey;

@Path("projects")
public class ProjectResource {

  @Inject
  private ProjectService projects;

  @GET
  public Response getProjects() {
    List<Project> projectlist = projects.getProjects(SecurityUtils.getSubject());
    if(projectlist == null) {
      return Response.status(Status.NOT_FOUND).entity(new ServerErrorResponseMessage("NoProjects")).build();
    }
    return Response.ok(projectlist).build();
  }

  @POST
  @Consumes("application/json")
  public Response addProject(Project project) {
    checkArgument(project != null);
    try {
      this.projects.addProject(project);
      return Response.created(UriBuilder.fromResource(ProjectResource.class).path(project.getKey()).build()).build();
    } catch(DuplicateKey e) {
      return Response.status(Status.BAD_REQUEST)
          .entity(new ServerErrorResponseMessage("ProjectExists", project.getKey())).build();
    }
  }

  @GET
  @Path("{projectKey}")
  public Response getIt(@PathParam("projectKey") String projectKey) {
    if(SecurityUtils.getSubject().isPermitted("project:" + projectKey) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage("Unauthorized")).build();
    }
    Project project = projects.where(QProject.project.key.eq(projectKey)).uniqueResult();
    if(project == null) {
      return Response.status(Status.NOT_FOUND).entity(new ServerErrorResponseMessage("NoSuchProject", projectKey))
          .build();
    }
    return ResponseTimestamper.ok(project).build();
  }

  @PUT
  @Path("{projectKey}")
  public Response updateProject(@PathParam("projectKey") String projectKey, Project project, @Context Request req) {
    checkArgument(project != null);
    if(SecurityUtils.getSubject().isPermitted("project:" + projectKey) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage("Unauthorized")).build();
    }
    ResponseTimestamper.evaluate(req, project);

    // update project use morphia query
    UpdateOperations<Project> ops =
        projects.datastore().createUpdateOperations(Project.class).set("name", project.getName());
    Query<Project> updateQuery = projects.datastore().createQuery(Project.class).field("key").equal(projectKey);

    projects.datastore().update(updateQuery, ops);

    return ResponseTimestamper.ok(project).build();
  }

  @GET
  @Path("{projectKey}/releases")
  public Response getReleases(@PathParam("projectKey") String projectKey) {
    if(SecurityUtils.getSubject().isPermitted("project:" + projectKey) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage("Unauthorized")).build();
    }
    Project project = projects.where(QProject.project.key.eq(projectKey)).uniqueResult();
    if(project == null) {
      return Response.status(Status.NOT_FOUND).entity(new ServerErrorResponseMessage("NoSuchProject", projectKey))
          .build();
    }
    return Response.ok(projects.getReleases(project)).build();
  }
}
