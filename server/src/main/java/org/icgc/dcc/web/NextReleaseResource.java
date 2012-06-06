package org.icgc.dcc.web;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;
import org.icgc.dcc.service.NextRelease;
import org.icgc.dcc.service.ReleaseService;

import com.google.inject.Inject;

@Path("nextRelease")
public class NextReleaseResource {

  @Inject
  private ReleaseService releaseService;

  @GET
  public Response getNextRelease() {
    NextRelease nextRelease = releaseService.getNextRelease();
    return Response.ok(nextRelease.getRelease()).build();
  }

  @POST
  public Response release(Release nextRelease) {
    NextRelease oldRelease = releaseService.getNextRelease();
    NextRelease newRelease = oldRelease.release(nextRelease);

    // save to mongoDB
    releaseService.getDatastore().save(oldRelease.getRelease());
    releaseService.getDatastore().save(newRelease.getRelease());

    return Response.ok(newRelease).build();
  }

  @GET
  @Path("queue")
  public Response getQueue() {
    List<String> projectIds = new ArrayList<String>();

    for(Submission submission : releaseService.getQueued()) {
      projectIds.add(submission.getProject().getAccessionId());
    }

    return Response.ok(projectIds).build();
  }

  @POST
  @Path("queue")
  public Response queue(List<String> accessionIds) {

    for(String accessionId : accessionIds) {
      for(Submission submission : releaseService.getSubmissionFromAccessionId(accessionId)) {
        submission.setState(SubmissionState.QUEUED);
        // save to mongoDB
        releaseService.getDatastore().save(submission);
      }
    }
    return Response.ok().build();
  }

  @DELETE
  @Path("queue")
  public Response removeAllQueued() {
    for(Submission submission : releaseService.getQueued()) {
      submission.setState(SubmissionState.NOT_VALIDATED);
      // save to mongoDB
      releaseService.getDatastore().save(submission);
    }
    return Response.ok().build();
  }

  @GET
  @Path("signed")
  public Response getSginedOff() {
    List<String> projectIds = new ArrayList<String>();

    for(Submission submission : releaseService.getSignedOff()) {
      projectIds.add(submission.getProject().getAccessionId());
    }

    return Response.ok(projectIds).build();
  }

  @POST
  @Path("signed")
  public Response signOff(List<String> accessionIds) {
    for(String accessionId : accessionIds) {
      for(Submission submission : releaseService.getSubmissionFromAccessionId(accessionId)) {
        submission.setState(SubmissionState.SIGNED_OFF);
        // save to mongoDB
        releaseService.getDatastore().save(submission);
      }
    }
    return Response.ok().build();

  }
}
