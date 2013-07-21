/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.web;

import static org.icgc.dcc.submission.web.Authorizations.getSubject;
import static org.icgc.dcc.submission.web.Authorizations.hasReleaseViewPrivilege;
import static org.icgc.dcc.submission.web.Authorizations.hasSpecificProjectPrivilege;
import static org.icgc.dcc.submission.web.Authorizations.isOmnipotentUser;
import static org.icgc.dcc.submission.web.Authorizations.unauthorizedResponse;
import static org.icgc.dcc.submission.web.Resources.noSuchEntityResponse;

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

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.shiro.subject.Subject;
import org.codehaus.jackson.map.annotate.JsonView;
import org.icgc.dcc.submission.core.model.Views.Digest;
import org.icgc.dcc.submission.fs.SubmissionFile;
import org.icgc.dcc.submission.release.ReleaseService;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.validation.report.FieldReport;
import org.icgc.dcc.submission.validation.report.SchemaReport;
import org.icgc.dcc.submission.validation.report.SubmissionReport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.inject.Inject;

@Slf4j
@Path("releases")
public class ReleaseResource {

  @Inject
  private ReleaseService releaseService;

  // TODO: This method seems like it should be removed since it is being exposed just for testing
  @VisibleForTesting
  @PUT
  public Response initialize(
      @Valid
      Release release,

      @Context
      Request request,

      @Context
      SecurityContext securityContext
      )
  {
    log.info("Initializing releases with: {}", release);
    if (isOmnipotentUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    if (release != null) {
      ResponseTimestamper.evaluate(request, release);

      if (this.releaseService.list().isEmpty()) {
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

  @GET
  @JsonView(Digest.class)
  public Response getReleases(
      @Context
      SecurityContext securityContext
      )
  {
    log.debug("Getting (filtered) releases");
    if (hasReleaseViewPrivilege(securityContext) == false) {
      return unauthorizedResponse();
    }

    Subject subject = getSubject(securityContext);
    List<Release> filteredReleases = this.releaseService.getReleasesBySubject(subject);

    return Response.ok(filteredReleases).build();
  }

  @GET
  @Path("{name}")
  public Response getReleaseByName(
      @PathParam("name")
      String name,

      @Context
      SecurityContext securityContext)
  {
    log.debug("Getting release using: {}", name);
    Subject subject = getSubject(securityContext);
    val releaseView = releaseService.getReleaseViewBySubject(name, subject);

    if (releaseView.isPresent() == false) {
      return noSuchEntityResponse(name);
    }

    return Response.ok(releaseView.get()).build();
  }

  @GET
  @Path("{name}/submissions/{projectKey}")
  public Response getSubmission(
      @PathParam("name")
      String releaseName,

      @PathParam("projectKey")
      String projectKey,

      @Context
      SecurityContext securityContext)
  {
    log.debug("Getting detailed submission: {}.{}", releaseName, projectKey);
    if (hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
      return unauthorizedResponse();
    }

    // TODO: use Optional...
    DetailedSubmission detailedSubmission = this.releaseService.getDetailedSubmission(releaseName, projectKey);
    if (detailedSubmission == null) {
      return noSuchEntityResponse(releaseName, projectKey);
    }

    return Response.ok(detailedSubmission).build();
  }

  @GET
  @Path("{name}/submissions/{projectKey}/report")
  public Response getSubmissionReport(
      @PathParam("name")
      String releaseName,

      @PathParam("projectKey")
      String projectKey,

      @Context
      SecurityContext securityContext)
  {
    log.debug("Getting submission report for: {}.{}", releaseName, projectKey);
    if (hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
      return unauthorizedResponse();
    }

    // TODO: use Optional...
    Submission submission = this.releaseService.getSubmission(releaseName, projectKey);
    if (submission == null) {
      return noSuchEntityResponse(releaseName, projectKey);
    }

    // DCC-799: Runtime type will be SubmissionReport. Static type is Object to untangle cyclic dependencies between
    // dcc-submission-server and dcc-submission-core.
    SubmissionReport report = (SubmissionReport) submission.getReport();
    return Response.ok(report).build();
  }

  @GET
  @Path("{name}/submissions/{projectKey}/report/{schema}")
  public Response getSchemaReport(
      @PathParam("name")
      String releaseName,

      @PathParam("projectKey")
      String projectKey,

      @PathParam("schema")
      String schema,

      @Context
      SecurityContext securityContext)
  {
    log.debug("Getting schema report for: {}.{}.{}", new Object[] { releaseName, projectKey, schema });
    if (hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
      return unauthorizedResponse();
    }

    val schemaReport = getSchemaReport(releaseName, projectKey, schema);
    if (schemaReport.isPresent() == false) {
      return noSuchEntityResponse(releaseName, projectKey, schema);
    }

    return Response.ok(schemaReport.get()).build();
  }

  @GET
  @Path("{name}/submissions/{projectKey}/report/{schema}/{field}")
  public Response getFieldReport(
      @PathParam("name")
      String releaseName,

      @PathParam("projectKey")
      String projectKey,

      @PathParam("schema")
      String schema,

      @PathParam("field")
      String field,

      @Context
      SecurityContext securityContext)
  {
    log.debug("Getting field report for: {}.{}.{}.{}", new Object[] { releaseName, projectKey, schema, field });
    if (hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
      return unauthorizedResponse();
    }

    Optional<SchemaReport> optionalSchemaReport = getSchemaReport(releaseName, projectKey, schema);
    if (optionalSchemaReport.isPresent() == false) {
      return noSuchEntityResponse(releaseName, projectKey, schema);
    }

    SchemaReport schemaReport = optionalSchemaReport.get();
    Optional<FieldReport> optionalFieldReport = schemaReport.getFieldReport(field);
    if (optionalFieldReport.isPresent() == false) {
      return noSuchEntityResponse(releaseName, projectKey, schema);
    }

    FieldReport fieldReport = optionalFieldReport.get();
    if (fieldReport == null) {
      return noSuchEntityResponse(releaseName, projectKey, schema);
    }

    return Response.ok(fieldReport).build();
  }

  @GET
  @Path("{name}/submissions/{projectKey}/files")
  public Response getSubmissionFileList(
      @PathParam("name")
      String releaseName,

      @PathParam("projectKey")
      String projectKey,

      @Context
      SecurityContext securityContext)
  {
    log.debug("Getting submission file list for release {} and project {}", releaseName, projectKey);
    if (hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
      return unauthorizedResponse();
    }

    Submission submission = this.releaseService.getSubmission(releaseName, projectKey);
    if (submission == null) { // TODO: use Optional...
      return noSuchEntityResponse(releaseName, projectKey);
    }

    List<SubmissionFile> submissionFiles = this.releaseService.getSubmissionFiles(releaseName, projectKey);
    return Response.ok(submissionFiles).build();
  }

  private Optional<SchemaReport> getSchemaReport(String releaseName, String projectKey, String schema) {
    Optional<SchemaReport> optional = Optional.absent();
    Submission submission = this.releaseService.getSubmission(releaseName, projectKey);
    if (submission != null) {
      // DCC-799: Runtime type will be SubmissionReport. Static type is Object to untangle cyclic dependencies between
      // dcc-submission-server and dcc-submission-core.
      SubmissionReport report = (SubmissionReport) submission.getReport();
      if (report != null) {
        SchemaReport schemaReport = report.getSchemaReport(schema);
        optional = Optional.of(schemaReport);
      }
    }

    return optional;
  }

}
