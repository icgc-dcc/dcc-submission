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

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.NO_SUCH_ENTITY;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.RELEASE_EXCEPTION;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.UNAVAILABLE;
import static org.icgc.dcc.submission.web.util.Authorizations.hasReleaseClosePrivilege;
import static org.icgc.dcc.submission.web.util.Authorizations.hasReleaseModifyPrivilege;
import static org.icgc.dcc.submission.web.util.Authorizations.hasReleaseViewPrivilege;
import static org.icgc.dcc.submission.web.util.Authorizations.hasSpecificProjectPrivilege;
import static org.icgc.dcc.submission.web.util.Authorizations.hasSubmissionSignoffPrivilege;
import static org.icgc.dcc.submission.web.util.Authorizations.isSuperUser;
import static org.icgc.dcc.submission.web.util.Responses.badRequest;
import static org.icgc.dcc.submission.web.util.Responses.noContent;
import static org.icgc.dcc.submission.web.util.Responses.unauthorizedResponse;

import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.glassfish.grizzly.http.util.Header;
import org.icgc.dcc.submission.core.model.DccModelOptimisticLockException;
import org.icgc.dcc.submission.core.model.InvalidStateException;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.release.NextRelease;
import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.release.ReleaseService;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.ValidationScheduler;
import org.icgc.dcc.submission.web.model.ServerErrorCode;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;
import org.icgc.dcc.submission.web.util.Authorizations;
import org.icgc.dcc.submission.web.util.ResponseTimestamper;
import org.icgc.dcc.submission.web.util.Responses;

import com.google.common.base.Joiner;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.typesafe.config.Config;

@Slf4j
@Path("nextRelease")
public class NextReleaseResource {

  private static final Joiner JOINER = Joiner.on("/");

  @Inject
  protected Config config;
  @Inject
  protected ReleaseService releaseService;
  @Inject
  protected ValidationScheduler validationScheduler;

  @GET
  public Response getNextRelease(

      @Context SecurityContext securityContext

      )
  {
    log.debug("Getting nextRelease");
    if (hasReleaseViewPrivilege(securityContext) == false) {
      return unauthorizedResponse();
    }

    String prefix = config.getString("http.ws.path");
    String redirectionPath = JOINER.join(
        prefix,
        "releases",
        fetchOpenRelease() // guaranteed not to be null
            .getName());

    return Response.status(Status.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, redirectionPath).build();
  }

  /**
   * Returns the current dictionary.
   * <p>
   * More: <code>{@link ReleaseService#getNextDictionary()}</code><br/>
   * Open-access intentional (DCC-758)
   */
  @GET
  @Path("dictionary")
  public Response getDictionary(

      @Context Request request

      )
  {
    Dictionary dictionary = releaseService.getNextDictionary();

    ResponseTimestamper.evaluate(request, dictionary);
    return ResponseTimestamper.ok(dictionary).build();
  }

  @POST
  public Response release(

      Release nextRelease,

      @Context Request request,

      @Context SecurityContext securityContext

      )
  {
    log.info("Releasing nextRelease, new release will be: {}", nextRelease);

    // TODO: This is intentionally not validated, since we're only currently using the name. This seems sketchy to me
    if (hasReleaseClosePrivilege(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    NextRelease oldRelease = releaseService.resolveNextRelease(); // guaranteed not null
    Release release = oldRelease.getRelease();
    String oldReleaseName = release.getName();
    log.info("Releasing {}", oldReleaseName);

    // Check the timestamp of the oldRelease, since that is the object being updated
    ResponseTimestamper.evaluate(request, release);

    NextRelease newRelease = null;
    try {
      newRelease = oldRelease.release(nextRelease.getName());
      log.info("Released {}", oldReleaseName);
    } catch (ReleaseException e) {
      ServerErrorCode code = RELEASE_EXCEPTION;
      log.error(code.getFrontEndString(), e);
      return Response.status(BAD_REQUEST).entity(new ServerErrorResponseMessage(code)).build();
    } catch (InvalidStateException e) {
      ServerErrorCode code = e.getCode();
      log.error(code.getFrontEndString(), e);
      return Response.status(BAD_REQUEST).entity(new ServerErrorResponseMessage(code)).build();
    }
    return ResponseTimestamper.ok(newRelease.getRelease()).build();
  }

  @GET
  @Path("queue")
  public Response getQueue() { // no authorization check needed (see DCC-808)
    /* no authorization check necessary */

    log.debug("Getting the queue for nextRelease");
    NextRelease nextRelease = releaseService.resolveNextRelease();
    List<String> projectIds = nextRelease.getQueued(); // TODO: ensure cannot be null (DCC-820)
    Object[] projectIdArray = projectIds.toArray();

    return Response.ok(projectIdArray).build();
  }

  @POST
  @Path("queue")
  public Response queue(

      @Valid List<QueuedProject> queuedProjects,

      @Context Request request,

      @Context SecurityContext securityContext

      )
  {
    log.info("Enqueuing projects for nextRelease: {}", queuedProjects);
    List<String> projectKeys = newArrayList();
    for (val queuedProject : queuedProjects) {
      val projectKey = queuedProject.getKey();
      if (hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
        return unauthorizedResponse();
      }

      projectKeys.add(projectKey);
    }

    Release nextRelease = releaseService.resolveNextRelease().getRelease();
    ResponseTimestamper.evaluate(request, nextRelease);

    try {
      releaseService.queue(nextRelease, queuedProjects);
    } catch (ReleaseException e) {
      log.error("ProjectKeyNotFound", e);

      // FIXME: This isn't correct
      return badRequest(NO_SUCH_ENTITY, projectKeys);
    } catch (InvalidStateException e) {
      val code = e.getCode();
      val offendingState = e.getState();
      log.error(code.getFrontEndString(), e);

      return badRequest(code, offendingState);
    } catch (DccModelOptimisticLockException e) {
      // Not very likely
      val code = UNAVAILABLE;
      log.error(code.getFrontEndString(), e);

      return Response
          .status(SERVICE_UNAVAILABLE) //
          .header(Header.RetryAfter.toString(), 3) //
          .entity(new ServerErrorResponseMessage(code)).build();
    }

    return noContent();
  }

  @DELETE
  @Path("queue")
  public Response removeAllQueued(

      @Context SecurityContext securityContext

      )
  {
    log.info("Emptying queue for nextRelease");
    if (isSuperUser(securityContext) == false) {
      return unauthorizedResponse();
    }
    releaseService.deleteQueuedRequests();

    return Response.ok().build();
  }

  @DELETE
  @Path("validation/{projectKey}")
  @SneakyThrows
  public Response cancelValidation(

      @PathParam("projectKey") String projectKey,

      @Context SecurityContext securityContext

      )
  {
    log.info("Cancelling validation for {}", projectKey);
    if (!hasSpecificProjectPrivilege(securityContext, projectKey)) {
      return unauthorizedResponse();
    }

    try {
      validationScheduler.cancelValidation(projectKey);
    } catch (InvalidStateException e) {
      ServerErrorCode code = e.getCode();
      log.error(code.getFrontEndString(), e);
      return badRequest(code, e.getMessage());
    } catch (Throwable t) {
      log.error("Error cancelling validation for '" + projectKey + "':", t);
      throw t;
    }

    return Response.ok().build();
  }

  @GET
  @Path("signed")
  public Response getSignedOff() {
    /* no authorization check needed (see DCC-808) */

    log.debug("Getting signed off projects for nextRelease");
    List<String> projectIds = releaseService.getSignedOff();
    return Response.ok(projectIds.toArray()).build();
  }

  @POST
  @Path("signed")
  public Response signOff(

      List<String> projectKeys,

      @Context Request request,

      @Context SecurityContext securityContext

      )
  {
    log.info("Signing off projects {}", projectKeys);
    if (hasSubmissionSignoffPrivilege(securityContext) == false) {
      return unauthorizedResponse();
    }

    Release nextRelease = releaseService.resolveNextRelease().getRelease();
    ResponseTimestamper.evaluate(request, nextRelease);

    try {
      String username = Authorizations.getUsername(securityContext);
      releaseService.signOff(nextRelease, projectKeys, username);
    } catch (ReleaseException e) {
      ServerErrorCode code = ServerErrorCode.NO_SUCH_ENTITY;
      log.error(code.getFrontEndString(), e);
      return Response.status(Status.BAD_REQUEST).entity(new ServerErrorResponseMessage(code, projectKeys)).build();
    } catch (InvalidStateException e) {
      ServerErrorCode code = e.getCode();
      log.error(code.getFrontEndString(), e);
      return Response.status(Status.BAD_REQUEST).entity(new ServerErrorResponseMessage(code)).build();
    } catch (DccModelOptimisticLockException e) { // not very likely
      ServerErrorCode code = ServerErrorCode.UNAVAILABLE;
      log.error(code.getFrontEndString(), e);
      return Response.status(Status.SERVICE_UNAVAILABLE) //
          .header(Header.RetryAfter.toString(), 3) //
          .entity(new ServerErrorResponseMessage(code)).build();
    }

    return Response.ok().build();
  }

  /**
   * See {@link ReleaseService#update(Release)}.
   */
  @PUT
  @Path("update")
  public Response update(

      @Valid Release release, // TODO: only requires String (+change UI)

      @Context Request request,

      @Context SecurityContext securityContext

      )
  {
    log.info("Updating nextRelease with: {}", release);
    if (hasReleaseModifyPrivilege(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    if (release != null) {
      String name = release.getName();

      log.info("updating {}", name);
      ResponseTimestamper.evaluate(request, release);

      if (releaseService.list().isEmpty()) {
        return status(BAD_REQUEST).build();
      } else {
        String updatedName = release.getName();
        String updatedDictionaryVersion = release.getDictionaryVersion();
        Release updatedRelease = releaseService.update(updatedName, updatedDictionaryVersion);
        log.info("updated {}", name);

        return ResponseTimestamper.ok(updatedRelease).build();
      }
    } else {
      return Response.status(BAD_REQUEST).build();
    }
  }

  private Release fetchOpenRelease() {
    return releaseService.resolveNextRelease().getRelease();
  }

}
