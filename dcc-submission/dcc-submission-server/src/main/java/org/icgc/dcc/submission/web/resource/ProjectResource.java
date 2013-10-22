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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.ALREADY_EXISTS;
import static org.icgc.dcc.submission.web.util.Authorizations.getSubject;
import static org.icgc.dcc.submission.web.util.Authorizations.hasSpecificProjectPrivilege;
import static org.icgc.dcc.submission.web.util.Authorizations.isSuperUser;
import static org.icgc.dcc.submission.web.util.Responses.noSuchEntityResponse;
import static org.icgc.dcc.submission.web.util.Responses.unauthorizedResponse;

import java.util.List;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.shiro.subject.Subject;
import org.icgc.dcc.submission.core.ProjectService;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.core.model.QProject;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;
import org.icgc.dcc.submission.web.util.ResponseTimestamper;
import org.icgc.dcc.submission.web.util.Responses;

import com.google.inject.Inject;
import com.mongodb.MongoException.DuplicateKey;

@Slf4j
@Path("projects")
public class ProjectResource {

  @Inject
  private ProjectService projectService;

  @GET
  public Response getProjects(

      @Context
      SecurityContext securityContext

      )
  {
    // Authorization is handled by the filtering of projects below
    log.debug("Getting projects");
    Subject subject = getSubject(securityContext);

    List<Project> projectList = projectService.getProjectsBySubject(subject);
    if (projectList == null) { // TODO: use Optional (see DCC-820)
      projectList = newArrayList();
    }

    return Response.ok(projectList).build();
  }

  @POST
  @Consumes("application/json")
  public Response addProject(

      @Context
      SecurityContext securityContext,

      @Valid
      Project project

      )
  {
    log.info("Adding project {}", project);
    if (isSuperUser(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    checkArgument(project != null);
    try {
      this.projectService.addProject(project);

      val url = UriBuilder.fromResource(ProjectResource.class).path(project.getKey()).build();
      return Response
          .created(url)
          .build();
    } catch (DuplicateKey e) {
      return Response
          .status(BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(ALREADY_EXISTS, project.getKey()))
          .build();
    }
  }

  @GET
  @Path("{projectKey}")
  public Response getProject(

      @PathParam("projectKey")
      String projectKey,

      @Context
      SecurityContext securityContext

      )
  {
    log.debug("Getting project: {}", projectKey);
    if (hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
      return Responses.unauthorizedResponse();
    }

    val project = projectService.where(QProject.project.key.eq(projectKey)).uniqueResult();
    if (project == null) { // TODO: use Optional
      return noSuchEntityResponse(projectKey);
    }

    return ResponseTimestamper.ok(project).build();
  }

  /**
   * Only updates the name and alias (will ignore the rest)
   */
  @PUT
  @Path("{projectKey}")
  public Response updateProject(

      @PathParam("projectKey")
      String projectKey,

      @Valid
      Project project,
      @Context
      Request req,

      @Context
      SecurityContext securityContext

      )
  {
    log.info("Updating project {} with {}", projectKey, project);
    if (isSuperUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    ResponseTimestamper.evaluate(req, project); // FIXME...

    // update project use morphia query
    projectService.datastore().update(
        projectService.datastore().createQuery(Project.class).field("key").equal(projectKey),
        projectService.datastore().createUpdateOperations(Project.class) //
            .set("name", project.getName())
            .set("alias", project.getAlias()));

    return ResponseTimestamper.ok(project).build();
  }

  @GET
  @Path("{projectKey}/releases")
  public Response getReleases(

      @PathParam("projectKey")
      String projectKey,

      @Context
      SecurityContext securityContext

      )
  {
    log.debug("Getting releases for project: {}", projectKey);
    if (hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
      return Responses.unauthorizedResponse();
    }

    // TODO: Encapsulate
    val project = projectService.where(QProject.project.key.eq(projectKey)).uniqueResult();
    if (project == null) {
      return noSuchEntityResponse(projectKey);
    }

    val releases = projectService.getReleases(project);

    return Response.ok(releases).build();
  }

}
