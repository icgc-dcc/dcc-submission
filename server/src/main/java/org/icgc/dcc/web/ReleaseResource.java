package org.icgc.dcc.web;

import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.icgc.dcc.filesystem.SubmissionFile;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.DetailedSubmission;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseView;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.shiro.AuthorizationPrivileges;
import org.icgc.dcc.shiro.ShiroSecurityContext;
import org.icgc.dcc.validation.report.FieldReport;
import org.icgc.dcc.validation.report.SchemaReport;
import org.icgc.dcc.validation.report.SubmissionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

@Path("releases")
public class ReleaseResource {

  private static final Logger log = LoggerFactory.getLogger(ReleaseResource.class);

  @Inject
  private ReleaseService releaseService;

  @GET
  public Response getResources(@Context SecurityContext securityContext) {
    log.debug("getting releases");
    List<Release> releases = this.releaseService.getReleases(((ShiroSecurityContext) securityContext).getSubject());
    return Response.ok(releases).build();
  }

  @GET
  @Path("{name}")
  public Response getReleaseByName(@PathParam("name") String name, @Context SecurityContext securityContext) {
    ReleaseView release = releaseService.getReleaseView(name, ((ShiroSecurityContext) securityContext).getSubject());

    if(release == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, name)).build();
    }
    return Response.ok(release).build();
  }

  @PUT
  public Response initialize(@Valid Release release, @Context Request req) {
    if(release != null) {
      ResponseTimestamper.evaluate(req, release);

      if(this.releaseService.list().isEmpty()) {
        this.releaseService.createInitialRelease(release);
        return ResponseTimestamper.ok(release).build();
      } else {
        return Response.status(Status.BAD_REQUEST)
            .entity(new ServerErrorResponseMessage(ServerErrorCode.ALREADY_INITIALIZED)).build();
      }
    } else {
      return Response.status(Status.BAD_REQUEST).entity(new ServerErrorResponseMessage(ServerErrorCode.EMPTY_REQUEST))
          .build();
    }
  }

  /*
   * // web service for adding submission to release (testing use only)
   * 
   * @POST
   * 
   * @Consumes("application/json")
   * 
   * @Path("{name}") public Response addSubmission(@PathParam("name") String name, Submission submission) {
   * checkArgument(submission != null);
   * 
   * UpdateOperations<Release> ops =
   * this.releaseService.getDatastore().createUpdateOperations(Release.class).add("submissions", submission);
   * 
   * Query<Release> updateQuery =
   * this.releaseService.getDatastore().createQuery(Release.class).field("name").equal(name);
   * 
   * this.releaseService.getDatastore().update(updateQuery, ops);
   * 
   * return Response.ok(submission).build(); }
   */

  @GET
  @Path("{name}/submissions/{projectKey}")
  public Response getSubmission(@PathParam("name") String name, @PathParam("projectKey") String projectKey,
      @Context SecurityContext securityContext) {
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.projectViewPrivilege(projectKey)) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    DetailedSubmission submission = this.releaseService.getDetailedSubmission(name, projectKey);

    if(submission == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, name, projectKey)).build();
    }
    return Response.ok(submission).build();
  }

  @GET
  @Path("{name}/submissions/{projectKey}/report")
  public Response getSubmissionReport(@PathParam("name") String name, @PathParam("projectKey") String projectKey,
      @Context SecurityContext securityContext) {
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.projectViewPrivilege(projectKey)) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    Submission submission = this.releaseService.getSubmission(name, projectKey);
    if(submission == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, name, projectKey)).build();
    }
    SubmissionReport report = submission.getReport();
    return Response.ok(report).build();
  }

  @GET
  @Path("{name}/submissions/{projectKey}/report/{schema}")
  public Response getSchemaReport(@PathParam("name") String name, @PathParam("projectKey") String projectKey,
      @PathParam("schema") String schema, @Context SecurityContext securityContext) {
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.projectViewPrivilege(projectKey)) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    Submission submission = this.releaseService.getSubmission(name, projectKey);
    if(submission == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, name, projectKey)).build();
    }
    SubmissionReport report = submission.getReport();
    if(report == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, name, projectKey)).build();
    }
    SchemaReport schemaReport = report.getSchemaReport(schema);
    if(schemaReport == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, schema)).build();
    }
    return Response.ok(schemaReport).build();
  }

  @GET
  @Path("{name}/submissions/{projectKey}/report/{schema}/{field}")
  public Response getFieldReport(@PathParam("name") String name, @PathParam("projectKey") String projectKey,
      @PathParam("schema") String schema, @PathParam("field") String field, @Context SecurityContext securityContext) {
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.projectViewPrivilege(projectKey)) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    Submission submission = this.releaseService.getSubmission(name, projectKey);
    if(submission == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, name, projectKey)).build();
    }
    SubmissionReport report = submission.getReport();
    if(report == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, name, projectKey)).build();
    }
    SchemaReport schemaReport = report.getSchemaReport(schema);
    if(schemaReport == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, schema)).build();
    }
    if(schemaReport.getFieldReport(field).isPresent() == false) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, schema, field)).build();
    }
    FieldReport fieldReport = schemaReport.getFieldReport(field).get();
    if(fieldReport == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, field)).build();
    }
    return Response.ok(fieldReport).build();
  }

  @GET
  @Path("{name}/submissions/{projectKey}/files")
  public Response getSubmissionFileList(@PathParam("name") String releaseName,
      @PathParam("projectKey") String projectKey, @Context SecurityContext securityContext) {
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.projectViewPrivilege(projectKey)) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    Submission submission = this.releaseService.getSubmission(releaseName, projectKey);
    if(submission == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, releaseName, projectKey)).build();
    }

    List<SubmissionFile> submissionFiles = this.releaseService.getSubmissionFiles(releaseName, projectKey);
    return Response.ok(submissionFiles).build();
  }
}
