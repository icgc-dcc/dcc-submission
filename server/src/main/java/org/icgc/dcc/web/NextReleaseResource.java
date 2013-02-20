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

import org.glassfish.grizzly.http.util.Header;
import org.icgc.dcc.core.model.DccModelOptimisticLockException;
import org.icgc.dcc.core.model.InvalidStateException;
import org.icgc.dcc.release.NextRelease;
import org.icgc.dcc.release.ReleaseException;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.QueuedProject;
import org.icgc.dcc.release.model.Release;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.typesafe.config.Config;

import static org.icgc.dcc.web.Authorizations.hasReleaseClosePrivilege;
import static org.icgc.dcc.web.Authorizations.hasReleaseModifyPrivilege;
import static org.icgc.dcc.web.Authorizations.hasReleaseViewPrivilege;
import static org.icgc.dcc.web.Authorizations.hasSpecificProjectPrivilege;
import static org.icgc.dcc.web.Authorizations.hasSubmissionSignoffPrivilege;
import static org.icgc.dcc.web.Authorizations.isOmnipotentUser;
import static org.icgc.dcc.web.Authorizations.unauthorizedResponse;

@Path("nextRelease")
public class NextReleaseResource {

  private static final Logger log = LoggerFactory.getLogger(NextReleaseResource.class);

  @Inject
  private Config config;

  @Inject
  private ReleaseService releaseService;

  @GET
  public Response getNextRelease(@Context SecurityContext securityContext) {
    log.debug("Getting nextRelease");
    if(hasReleaseViewPrivilege(securityContext) == false) {
      return unauthorizedResponse();
    }
    NextRelease nextRelease = releaseService.getNextRelease();
    Release release = nextRelease.getRelease(); // guaranteed not to be null
    String prefix = config.getString("http.ws.path");
    String redirectionPath = Joiner.on("/").join(prefix, "releases", release.getName());
    return Response.status(Status.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, redirectionPath).build();
  }

  @POST
  public Response release(Release nextRelease, @Context Request req, @Context SecurityContext securityContext) {
    log.info("Releasing nextRelease, new release will be: {}", nextRelease);

    // TODO: this is intentionally not validated, since we're only currently using the name. This seems sketchy to me
    // --Jonathan (DCC-759)
    if(hasReleaseClosePrivilege(securityContext) == false) {
      return unauthorizedResponse();
    }

    NextRelease oldRelease = releaseService.getNextRelease(); // guaranteed not null
    Release release = oldRelease.getRelease();
    String oldReleaseName = release.getName();
    log.info("Releasing {}", oldReleaseName);

    // Check the timestamp of the oldRelease, since that is the object being updated
    ResponseTimestamper.evaluate(req, release);

    NextRelease newRelease = null;
    try {
      newRelease = oldRelease.release(nextRelease.getName());
      log.info("Released {}", oldReleaseName);
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
  public Response getQueue() { // no authorization check needed (see DCC-808)
    /* no authorization check necessary */

    log.debug("Getting the queue for nextRelease");
    List<String> projectIds = releaseService.getNextRelease().getQueued();
    return Response.ok(projectIds.toArray()).build();
  }

  @POST
  @Path("queue")
  public Response queue(@Valid List<QueuedProject> queuedProjects, @Context Request req,
      @Context SecurityContext securityContext) {

    log.info("Enqueuing projects for nextRelease: {}", queuedProjects);
    List<String> projectKeys = Lists.newArrayList();
    for(QueuedProject qp : queuedProjects) {
      String projectKey = qp.getKey();
      if(hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
        return unauthorizedResponse();
      }

      projectKeys.add(projectKey);
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

    log.info("Emptying queue for nextRelease");
    if(isOmnipotentUser(securityContext) == false) {
      return unauthorizedResponse();
    }
    this.releaseService.deleteQueuedRequest();

    return Response.ok().build();
  }

  @GET
  @Path("signed")
  public Response getSignedOff() {
    /* no authorization check needed (see DCC-808) */

    log.debug("Getting signed off projects for nextRelease");
    List<String> projectIds = this.releaseService.getSignedOff();
    return Response.ok(projectIds.toArray()).build();
  }

  @POST
  @Path("signed")
  public Response signOff(List<String> projectKeys, @Context Request req, @Context SecurityContext securityContext) {
    log.info("Signing off projects {}", projectKeys);
    if(hasSubmissionSignoffPrivilege(securityContext) == false) {
      return unauthorizedResponse();
    }

    Release nextRelease = this.releaseService.getNextRelease().getRelease();
    ResponseTimestamper.evaluate(req, nextRelease);

    try {
      String username = Authorizations.getUsername(securityContext);
      this.releaseService.signOff(nextRelease, projectKeys, username);
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
    log.info("Updating nextRelease with: {}", release);
    if(hasReleaseModifyPrivilege(securityContext) == false) {
      return unauthorizedResponse();
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
