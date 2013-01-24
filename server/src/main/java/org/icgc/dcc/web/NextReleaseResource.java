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

import org.apache.shiro.subject.Subject;
import org.glassfish.grizzly.http.util.Header;
import org.icgc.dcc.core.model.DccModelOptimisticLockException;
import org.icgc.dcc.core.model.InvalidStateException;
import org.icgc.dcc.release.NextRelease;
import org.icgc.dcc.release.ReleaseException;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.QueuedProject;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.shiro.AuthorizationPrivileges;
import org.icgc.dcc.shiro.ShiroSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

@Path("nextRelease")
public class NextReleaseResource {

  private static final Logger log = LoggerFactory.getLogger(NextReleaseResource.class);

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
    String oldReleaseName = oldRelease.getRelease().getName();
    log.info("releasing {}", oldReleaseName);

    // Check the timestamp of the oldRelease, since that is the object being updated
    ResponseTimestamper.evaluate(req, oldRelease.getRelease());

    NextRelease newRelease = null;
    try {
      newRelease = oldRelease.release(nextRelease.getName());
      log.info("released {}", oldReleaseName);
    } catch(ReleaseException e) {
      ServerErrorCode code = ServerErrorCode.RELEASE_EXCEPTION;
      log.error(code.getFrontEndString(), e);
      return Response.status(Status.BAD_REQUEST).entity(new ServerErrorResponseMessage(code)).build();
    } catch(InvalidStateException e) {
      ServerErrorCode code = e.getCode();
      log.error(code.getFrontEndString(), e);
      return Response.status(Status.BAD_REQUEST).entity(new ServerErrorResponseMessage(code)).build();
    }
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

    Subject subject = ((ShiroSecurityContext) securityContext).getSubject();
    List<String> projectKeys = Lists.newArrayList();
    for(QueuedProject qp : queuedProjects) {
      String projectKey = qp.getKey();
      projectKeys.add(projectKey);

      if(subject.isPermitted(AuthorizationPrivileges.projectViewPrivilege(projectKey)) == false) {
        return Response.status(Status.UNAUTHORIZED)
            .entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED)).build();
      }
    }

    Release nextRelease = this.releaseService.getNextRelease().getRelease();
    ResponseTimestamper.evaluate(req, nextRelease);

    try {
      this.releaseService.queue(nextRelease, queuedProjects);
    } catch(ReleaseException e) {
      log.error("ProjectKeyNotFound", e);
      return Response.status(Status.BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, projectKeys)).build();
    } catch(InvalidStateException e) {
      ServerErrorCode code = e.getCode();
      log.error(code.getFrontEndString(), e);
      return Response.status(Status.BAD_REQUEST).entity(new ServerErrorResponseMessage(code)).build();
    } catch(DccModelOptimisticLockException e) { // not very likely
      ServerErrorCode code = ServerErrorCode.UNAVAILABLE;
      log.error(code.getFrontEndString(), e);
      return Response.status(Status.SERVICE_UNAVAILABLE) //
          .header(Header.RetryAfter.toString(), 3) //
          .entity(new ServerErrorResponseMessage(code)).build();
    }
    return Response.ok().build();
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
    ShiroSecurityContext context = (ShiroSecurityContext) securityContext;
    Subject subject = context.getSubject();
    String user = context.getUserPrincipal().getName();

    if(subject.isPermitted(AuthorizationPrivileges.RELEASE_SIGNOFF.getPrefix()) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }

    Release nextRelease = this.releaseService.getNextRelease().getRelease();
    ResponseTimestamper.evaluate(req, nextRelease);

    try {
      this.releaseService.signOff(nextRelease, projectKeys, user);
    } catch(ReleaseException e) {
      ServerErrorCode code = ServerErrorCode.NO_SUCH_ENTITY;
      log.error(code.getFrontEndString(), e);
      return Response.status(Status.BAD_REQUEST).entity(new ServerErrorResponseMessage(code, projectKeys)).build();
    } catch(InvalidStateException e) {
      ServerErrorCode code = e.getCode();
      log.error(code.getFrontEndString(), e);
      return Response.status(Status.BAD_REQUEST).entity(new ServerErrorResponseMessage(code)).build();
    } catch(DccModelOptimisticLockException e) { // not very likely
      ServerErrorCode code = ServerErrorCode.UNAVAILABLE;
      log.error(code.getFrontEndString(), e);
      return Response.status(Status.SERVICE_UNAVAILABLE) //
          .header(Header.RetryAfter.toString(), 3) //
          .entity(new ServerErrorResponseMessage(code)).build();
    }
    return Response.ok().build();
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
      String name = release.getName();

      log.info("updating {}", name);
      ResponseTimestamper.evaluate(req, release);

      if(this.releaseService.list().isEmpty()) {
        return Response.status(Status.BAD_REQUEST).build();
      } else {
        Release updatedRelease = releaseService.update(release);
        log.info("updated {}", name);

        return ResponseTimestamper.ok(updatedRelease).build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }
}