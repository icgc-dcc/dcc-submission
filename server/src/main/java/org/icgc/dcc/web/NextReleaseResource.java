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
import org.icgc.dcc.release.model.Release;

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
  public Response release(Release nextRelease, @Context Request req) {
    // check for submission state to be signed off
    if(!releaseService.getNextRelease().canRelease()) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    NextRelease oldRelease = releaseService.getNextRelease();
    // Check the timestamp of the oldRelease, since that is the object being updated
    ResponseTimestamper.evaluate(req, oldRelease.getRelease());
    NextRelease newRelease = oldRelease.release(nextRelease);

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

    this.releaseService.queue(projectKeys);
    if(true) {// TODO: acutally return false upon invalid request
      return Response.ok().build();
    } else {
      return Response.status(Status.BAD_REQUEST).build();
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

    this.releaseService.signOff(projectKeys);
    if(true) {// TODO: acutally return false upon invalid request
      return Response.ok().build();
    } else {
      return Response.status(Status.BAD_REQUEST).build();
    }
  }
}