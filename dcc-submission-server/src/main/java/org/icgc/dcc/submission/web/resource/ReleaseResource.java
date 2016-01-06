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
package org.icgc.dcc.submission.web.resource;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.ALREADY_INITIALIZED;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.EMPTY_REQUEST;
import static org.icgc.dcc.submission.web.util.Authorizations.getSubject;
import static org.icgc.dcc.submission.web.util.Authorizations.hasReleaseViewPrivilege;
import static org.icgc.dcc.submission.web.util.Authorizations.hasSpecificProjectPrivilege;
import static org.icgc.dcc.submission.web.util.Authorizations.isSuperUser;
import static org.icgc.dcc.submission.web.util.Responses.noSuchEntityResponse;
import static org.icgc.dcc.submission.web.util.Responses.unauthorizedResponse;

import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.icgc.dcc.submission.core.model.Views.Digest;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.service.ReleaseService;
import org.icgc.dcc.submission.service.SystemService;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;
import org.icgc.dcc.submission.web.util.ResponseTimestamper;
import org.icgc.dcc.submission.web.util.Responses;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("releases")
public class ReleaseResource {

  @Inject
  private ReleaseService releaseService;
  @Inject
  private SystemService systemService;

  // TODO: This method seems like it should be removed since it is being exposed just for testing
  @VisibleForTesting
  @PUT
  public Response initialize(
      @Valid Release release,
      @Context Request request,
      @Context SecurityContext securityContext) {
    log.info("Initializing releases with: {}", release);
    if (isSuperUser(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    if (release != null) {
      ResponseTimestamper.evaluate(request, release);

      val empty = releaseService.countOpenReleases() == 0;
      if (empty) {
        releaseService.createInitialRelease(release);

        return ResponseTimestamper
            .ok(release)
            .build();
      } else {
        return Response
            .status(BAD_REQUEST)
            .entity(new ServerErrorResponseMessage(ALREADY_INITIALIZED))
            .build();
      }
    } else {
      return Response
          .status(BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(EMPTY_REQUEST))
          .build();
    }
  }

  @GET
  @JsonView(Digest.class)
  public Response getReleases(@Context SecurityContext securityContext) {
    log.debug("Getting visible releases");
    if (hasReleaseViewPrivilege(securityContext) == false) {
      return unauthorizedResponse();
    }

    val subject = getSubject(securityContext);
    val visibileReleases = releaseService.getReleasesBySubject(subject);

    return Response.ok(visibileReleases).build();
  }

  @GET
  @Path("{name}")
  public Response getReleaseByName(
      @PathParam("name") String name,
      @Context SecurityContext securityContext) {
    log.debug("Getting release using: {}", name);
    val subject = getSubject(securityContext);
    val releaseView = releaseService.getReleaseViewBySubject(name, subject);

    if (releaseView.isPresent() == false) {
      return noSuchEntityResponse(name);
    }

    val result = releaseView.get();
    result.setLocked(!systemService.isEnabled());

    return Response.ok(result).build();
  }

  @GET
  @Path("{name}/submissions/{projectKey}")
  public Response getSubmission(
      @PathParam("name") String releaseName,
      @PathParam("projectKey") String projectKey,
      @Context SecurityContext securityContext) {
    log.debug("Getting detailed submission: {}.{}", releaseName, projectKey);
    if (hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
      return Responses.unauthorizedResponse();
    }

    // TODO: use Optional...
    DetailedSubmission detailedSubmission = releaseService.getDetailedSubmission(releaseName, projectKey);
    if (detailedSubmission == null) {
      return noSuchEntityResponse(releaseName, projectKey);
    }

    detailedSubmission.setLocked(!systemService.isEnabled());

    return Response.ok(detailedSubmission).build();
  }

  @GET
  @Path("{name}/submissions/{projectKey}/report")
  public Response getReport(
      @PathParam("name") String releaseName,
      @PathParam("projectKey") String projectKey,
      @Context SecurityContext securityContext) {
    log.debug("Getting submission report for: {}.{}", releaseName, projectKey);
    if (hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
      return Responses.unauthorizedResponse();
    }

    // TODO: use Optional...
    val submission = releaseService.getSubmission(releaseName, projectKey);
    if (submission == null) {
      return noSuchEntityResponse(releaseName, projectKey);
    }

    // DCC-799: Runtime type will be SubmissionReport. Static type is Object to untangle cyclic dependencies between
    // dcc-submission-server and dcc-submission-core.
    val report = submission.getReport();
    return Response.ok(report).build();
  }

  @GET
  @Path("{name}/submissions/{projectKey}/report/{fileName}")
  public Response getFileReport(
      @PathParam("name") String releaseName,
      @PathParam("projectKey") String projectKey,
      @PathParam("fileName") String fileName,
      @Context SecurityContext securityContext) {
    log.debug("Getting file report for: {}.{}.{}", new Object[] { releaseName, projectKey, fileName });
    if (hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
      return Responses.unauthorizedResponse();
    }

    val fileReport = releaseService.getFileReport(releaseName, projectKey, fileName);
    if (fileReport.isPresent() == false) {
      return noSuchEntityResponse(releaseName, projectKey, fileName);
    }

    return Response.ok(fileReport.get()).build();
  }

  @GET
  @Path("{name}/submissions/{projectKey}/files")
  public Response getSubmissionFileList(
      @PathParam("name") String releaseName,
      @PathParam("projectKey") String projectKey,
      @Context SecurityContext securityContext) {
    log.debug("Getting submission file list for release {} and project {}", releaseName, projectKey);
    if (hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
      return Responses.unauthorizedResponse();
    }

    if (!releaseService.submissionExists(releaseName, projectKey)) {
      return noSuchEntityResponse(releaseName, projectKey);
    }

    val submissionFiles = releaseService.getSubmissionFiles(releaseName, projectKey);
    return Response.ok(submissionFiles).build();
  }

}
