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
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.ALREADY_EXISTS;
import static org.icgc.dcc.submission.web.util.Authorizations.getSubject;
import static org.icgc.dcc.submission.web.util.Authorizations.hasSpecificProjectPrivilege;
import static org.icgc.dcc.submission.web.util.Authorizations.isSuperUser;

import java.util.Set;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.service.ProjectService;
import org.icgc.dcc.submission.service.ReleaseService;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;
import org.icgc.dcc.submission.web.util.Responses;

import com.google.inject.Inject;
import com.mongodb.MongoException.DuplicateKey;

@Slf4j
@Path("projects")
@Produces("application/json")
@Consumes("application/json")
public class ProjectResource {

  @Inject
  private ProjectService projectService;

  @Inject
  private ReleaseService releaseService;

  @Inject
  private DccFileSystem dccFileSystem;

  @GET
  public Response getProjects(@Context
  SecurityContext securityContext) {
    log.info("Request for all Projects");

    val user = getSubject(securityContext);
    Set<Project> projects;

    if (isSuperUser(securityContext)) {
      log.info("'{}' is super user", user.getPrincipal());
      projects = projectService.findAll();
    } else {
      log.info("'{}' is not super user", user.getPrincipal());
      projects = projectService.findAllForUser(user.getPrincipal().toString());
    }

    return Response.ok(projects).build();
  }

  @POST
  public Response addProject(@Context
  SecurityContext securityContext, @Valid
  Project project) {
    log.info("Request to add Project '{}'", project);

    val user = getSubject(securityContext);
    if (isSuperUser(securityContext) == false) {
      log.warn("'{}' is not super user", user.getPrincipal());
      return Responses.unauthorizedResponse();
    }
    log.info("'{}' is super user", user.getPrincipal());

    Response response;
    try {
      // Save Project to DB
      projectService.add(project);

      // Update Release and save to DB
      val release = releaseService.addSubmission(project.getKey(), project.getName());

      // Add directory for submission
      // TODO: move this to service
      String projectDirectoryPath =
          dccFileSystem.createNewProjectDirectoryStructure(release.getName(), project.getKey());

      response =
          Response.created(UriBuilder.fromResource(ProjectResource.class).path(project.getKey()).build()).build();
      log.info("Project '{}' added ({})!", project.getKey(), projectDirectoryPath);
    } catch (DuplicateKey e) {
      response = Response.status(BAD_REQUEST).entity(new ServerErrorResponseMessage(ALREADY_EXISTS, project.getKey()))
          .build();
      log.warn("Project '{}' already exists! Could NOT be added.", project.getKey());
    }

    return response;
  }

  @GET
  @Path("{projectKey}")
  public Response getProject(@PathParam("projectKey")
  String projectKey, @Context
  SecurityContext securityContext) {
    log.info("Request for Project '{}'", projectKey);

    val user = getSubject(securityContext);
    Project project;

    if (hasAccess(securityContext, projectKey) == false) {
      log.info("Project '{}' not visible to '{}'", projectKey, user.getPrincipal());
      return Responses.notFound(projectKey);
    }

    project = projectService.find(projectKey);

    if (project == null) {
      log.info("Project '{}' not found", projectKey);
      return Responses.notFound(projectKey);
    }

    return Response.ok(project).build();
  }

  @POST
  @Path("{projectKey}")
  public Response updateProject(
      @PathParam("projectKey")
      String projectKey,
      @Valid
      Project project,
      @Context
      SecurityContext securityContext) {
    log.info("Request to update Project '{}' with '{}'", projectKey, project);

    val user = getSubject(securityContext);
    if (isSuperUser(securityContext) == false) {
      log.warn("'{}' is not super user", user.getPrincipal());
      return Responses.unauthorizedResponse();
    }
    log.info("'{}' is super user", user.getPrincipal());

    if (!projectKey.equals(project.getKey())) {
      log.warn("Project key '{}' does not match endpoint for '{}'", project.getKey(), projectKey);
      return Response.status(PRECONDITION_FAILED).entity("Project Key Missmatch").build();
    }

    val result = projectService.update(project);

    return Response.ok(result).build();
  }

  @GET
  @Path("{projectKey}/releases")
  public Response getProjectSubmissions(
      @PathParam("projectKey")
      String projectKey,
      @Context
      SecurityContext securityContext) {
    log.info("Request for all Submissions from Project '{}'", projectKey);

    val user = getSubject(securityContext);

    if (hasAccess(securityContext, projectKey) == false) {
      log.warn("Project '{}' not visible to '{}'", projectKey, user.getPrincipal());
      return Responses.notFound(projectKey);
    }

    val releases = releaseService.findAll();
    val submissions = projectService.extractSubmissions(releases, projectKey);

    return Response.ok(submissions).build();
  }

  private boolean hasAccess(SecurityContext securityContext, String projectKey) {
    return projectKey != null && hasSpecificProjectPrivilege(securityContext, projectKey);
  }
}
