package org.icgc.dcc.web;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.icgc.dcc.release.NextRelease;
import org.icgc.dcc.release.ReleaseService;

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
  public Response release(String nextReleaseName, @Context Request req) {
    NextRelease oldRelease = releaseService.getNextRelease();
    // Check the timestamp of the oldRelease, since that is the object being updated
    ResponseTimestamper.evaluate(req, oldRelease.getRelease());
    NextRelease newRelease = oldRelease.release(nextReleaseName);

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
  public Response queue(List<String> projectKeys, @Context Request req) {
    ResponseTimestamper.evaluate(req, this.releaseService.getNextRelease().getRelease());

    if(this.releaseService.hasProjectKey(projectKeys)) {
      this.releaseService.queue(projectKeys);
      return Response.ok().build();
    } else {
      return Response.status(Status.BAD_REQUEST).entity(new ServerErrorResponseMessage("ProjectKeyNotFound")).build();
    }
  }

  @DELETE
  @Path("queue")
  public Response removeAllQueued() {
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
  public Response signOff(List<String> projectKeys, @Context Request req) {
    ResponseTimestamper.evaluate(req, this.releaseService.getNextRelease().getRelease());

    if(this.releaseService.hasProjectKey(projectKeys)) {
      this.releaseService.signOff(projectKeys);
      return Response.ok().build();
    } else {
      return Response.status(Status.BAD_REQUEST).entity(new ServerErrorResponseMessage("ProjectKeyNotFound")).build();
    }
  }
}