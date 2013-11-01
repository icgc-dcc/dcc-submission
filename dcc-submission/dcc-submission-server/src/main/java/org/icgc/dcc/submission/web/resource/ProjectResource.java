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
import static org.icgc.dcc.submission.web.model.ServerErrorCode.ALREADY_EXISTS;
import static org.icgc.dcc.submission.web.util.Authorizations.getSubject;
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

import org.apache.shiro.subject.Subject;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.repository.ProjectRepository;
import org.icgc.dcc.submission.shiro.AuthorizationPrivileges;
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
  private ProjectRepository projectRepository;

  final private boolean isAuthorized(Subject user, String projectKey) {
    return projectKey != null && user.isPermitted(AuthorizationPrivileges.projectViewPrivilege(projectKey));
  }

  @GET
  public Response getProjects(@Context
  SecurityContext securityContext) {
    val user = getSubject(securityContext);
    log.info("Request for all Projects from User [{}]", user.getPrincipal());

    Set<Project> projects;// = filterUnauthorized(user, projectRepository.findProjects());

    if (isSuperUser(securityContext)) {
      log.info("[{}] is super user", user.getPrincipal());
      projects = projectRepository.findProjects();
    } else {
      log.info("[{}] is not super user", user.getPrincipal());
      projects = projectRepository.findProjects(user);
    }

    return Response.ok(projects).build();
  }

  @POST
  public Response addProject(@Context
  SecurityContext securityContext, @Valid
  Project project) {
    val user = getSubject(securityContext);
    log.info("Request to add Project [{}] from [{}]", project, user.getPrincipal());

    if (isSuperUser(securityContext) == false) {
      log.info("[{}] is not super user", user.getPrincipal());
      return Responses.unauthorizedResponse();
    }
    log.info("[{}] is super user", user.getPrincipal());

    try {
      projectRepository.addProject(project);

      val url = UriBuilder.fromResource(ProjectResource.class).path(project.getKey()).build();
      return Response.created(url).build();
    } catch (DuplicateKey e) {
      return Response.status(BAD_REQUEST).entity(new ServerErrorResponseMessage(ALREADY_EXISTS, project.getKey()))
          .build();
    }
  }

  @GET
  @Path("{projectKey}")
  public Response getProject(@PathParam("projectKey")
  String projectKey, @Context
  SecurityContext securityContext) {
    val user = getSubject(securityContext);
    log.info("Request for Project [{}] from User [{}]", projectKey, user.getPrincipal());

    if (isAuthorized(user, projectKey) == false) {
      // This check doubles as a project existence check for non-admin users.
      // 404 instead of 401 to keep from leaking whether the project exists to an unauthorized user
      return Responses.notFound(projectKey);
    }

    val project = projectRepository.findProject(projectKey);

    if (project == null) {
      // This check is really only needed because admins bypass the check above
      return Responses.notFound(projectKey);
    }

    return Response.ok(project).build();
  }
}
