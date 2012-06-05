package org.icgc.dcc.web;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.icgc.dcc.model.QRelease;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.service.HasRelease;
import org.icgc.dcc.service.ReleaseService;

import com.google.inject.Inject;

@Path("releases")
public class ReleaseResource {

  @Inject
  private ReleaseService releaseService;

  @GET
  public Response getResources() {
    List<HasRelease> hasReleases = releaseService.list();
    List<Release> releases = new ArrayList<Release>();
    for(HasRelease hasRelease : hasReleases) {
      releases.add(hasRelease.getRelease());
    }
    return Response.ok(releases).build();
  }

  @GET
  @Path("{name}")
  public Response getReleaseByName(@PathParam("name") String name) {
    Release release = releaseService.where(QRelease.release.name.eq(name)).singleResult();
    if(release == null) {
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.ok(release).build();
  }

  @PUT
  @Path("{name}")
  public Response updateRelease() {
    return Response.ok().build();
  }

  @GET
  @Path("{name}/submissions/{accessionId}")
  public Response getSubmission(@PathParam("name") String name, @PathParam("accessionId") String accessionId) {
    Release release = releaseService.where(QRelease.release.name.eq(name)).singleResult();
    List<Submission> submissions = new ArrayList<Submission>();
    for(Submission submission : release.getSubmissions()) {
      if(submission.getProject().getAccessionId().equals(accessionId)) {
        submissions.add(submission);
      }
    }
    return Response.ok(submissions).build();
  }
}
