package org.icgc.dcc.web;

import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.icgc.dcc.release.NextRelease;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.QueuedProject;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.shiro.AuthorizationPrivileges;
import org.icgc.dcc.shiro.ShiroSecurityContext;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

@Path("nextRelease")
public class NextReleaseResource {

  @Inject
  private ReleaseService releaseService;

  @GET
  public Response getNextRelease() {
    NextRelease nextRelease = releaseService.getNextRelease();
    return ResponseTimestamper.ok(nextRelease.getRelease()).build();
  }

  @POST
  public Response release(Release nextRelease, @Context Request req, @Context SecurityContext securityContext) {
    // TODO: this is intentionally not validated, since we're only currently using the name. This seems sketchy to me
    // --Jonathan
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.RELEASE_CLOSE.getPrefix()) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    NextRelease oldRelease = releaseService.getNextRelease();
    // Check the timestamp of the oldRelease, since that is the object being updated
    ResponseTimestamper.evaluate(req, oldRelease.getRelease());
    NextRelease newRelease = oldRelease.release(nextRelease.getName());

    return ResponseTimestamper.ok(newRelease.getRelease()).build();
  }

  @GET
  @Path("queue")
  public Response getQueue() {
    List<String> projectIds = releaseService.getNextRelease().getQueued();

    return Response.ok(projectIds.toArray()).build();
  }

  @POST
  @Path("queue")
  public Response queue(@Valid List<QueuedProject> queuedProjects, @Context Request req,
      @Context SecurityContext securityContext) {

    List<String> projectKeys = Lists.newArrayList();

    for(QueuedProject qp : queuedProjects) {
      String projectKey = qp.getKey();
      projectKeys.add(projectKey);

      if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
          AuthorizationPrivileges.projectViewPrivilege(projectKey)) == false) {
        return Response.status(Status.UNAUTHORIZED)
            .entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED)).build();
      }
    }
    ResponseTimestamper.evaluate(req, this.releaseService.getNextRelease().getRelease());

    List<String> projectsInRelease = this.releaseService.getProjectKeys();
    if(projectsInRelease.containsAll(projectKeys)) {
      this.releaseService.queue(queuedProjects);
      return Response.ok().build();
    } else {
      projectKeys.removeAll(projectsInRelease);
      return Response.status(Status.BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, projectKeys)).build();
    }
  }

  @DELETE
  @Path("queue")
  public Response removeAllQueued(@Context SecurityContext securityContext) {
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.QUEUE_DELETE.getPrefix()) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    this.releaseService.deleteQueuedRequest();

    return Response.ok().build();
  }

  @GET
  @Path("signed")
  public Response getSignedOff() {
    List<String> projectIds = this.releaseService.getSignedOff();

    return Response.ok(projectIds).build();
  }

  @POST
  @Path("signed")
  public Response signOff(List<String> projectKeys, @Context Request req, @Context SecurityContext securityContext) {
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.RELEASE_SIGNOFF.getPrefix()) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    Release release = this.releaseService.getNextRelease().getRelease();
    ResponseTimestamper.evaluate(req, release);

    List<String> projectsInRelease = this.releaseService.getProjectKeys();
    if(projectsInRelease.containsAll(projectKeys)) {
      String user = ((ShiroSecurityContext) securityContext).getUserPrincipal().getName();
      this.releaseService.signOff(user, projectKeys, release.getName());
      return Response.ok().build();
    } else {
      projectKeys.removeAll(projectsInRelease);
      return Response.status(Status.BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, projectKeys)).build();
    }
  }

  @PUT
  @Path("update")
  public Response update(@Valid Release release, @Context Request req, @Context SecurityContext securityContext) {
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.RELEASE_MODIFY.getPrefix()) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    if(release != null) {
      ResponseTimestamper.evaluate(req, release);

      if(this.releaseService.list().isEmpty()) {
        return Response.status(Status.BAD_REQUEST).build();
      } else {
        Release updatedRelease = releaseService.update(release);
        return ResponseTimestamper.ok(updatedRelease).build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }
}