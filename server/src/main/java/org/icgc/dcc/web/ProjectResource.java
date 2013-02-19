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
import javax.ws.rs.Consumes;
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
import javax.ws.rs.core.UriBuilder;

import org.apache.shiro.subject.Subject;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.QProject;
import org.icgc.dcc.shiro.ShiroSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mongodb.MongoException.DuplicateKey;

import static com.google.common.base.Preconditions.checkArgument;
import static org.icgc.dcc.web.Authorizations.hasSpecificProjectPrivilege;
import static org.icgc.dcc.web.Authorizations.isOmnipotentUser;
import static org.icgc.dcc.web.Authorizations.unauthorizedResponse;
import static org.icgc.dcc.web.Resources.noSuchEntityResponse;

@Path("projects")
public class ProjectResource {

  private static final Logger log = LoggerFactory.getLogger(ProjectResource.class);

  @Inject
  private ProjectService projects;

  @GET
  public Response getProjects(@Context SecurityContext securityContext) {
    /* Authorization is handled by the filtering of projects below */

    log.debug("Getting projects");
    Subject subject = ((ShiroSecurityContext) securityContext).getSubject();
    List<Project> projectList = projects.getFilteredProjects(subject);
    if(projectList == null) { // TODO: use Optional (see DCC-820)
      projectList = Lists.newArrayList();
    }
    return Response.ok(projectList).build();
  }

  @POST
  @Consumes("application/json")
  public Response addProject(@Context SecurityContext securityContext, @Valid Project project) {

    log.info("Adding project {}", project);
    if(isOmnipotentUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    checkArgument(project != null);
    try {
      this.projects.addProject(project);
      return Response.created(UriBuilder.fromResource(ProjectResource.class).path(project.getKey()).build()).build();
    } catch(DuplicateKey e) {
      return Response.status(Status.BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.ALREADY_EXISTS, project.getKey())).build();
    }
  }

  @GET
  @Path("{projectKey}")
  public Response getRessource(@PathParam("projectKey") String projectKey, @Context SecurityContext securityContext) {

    log.debug("Getting project: {}", projectKey);
    if(hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
      return unauthorizedResponse();
    }

    Project project = projects.where(QProject.project.key.eq(projectKey)).uniqueResult();
    if(project == null) { // TODO: use Optional
      return noSuchEntityResponse(projectKey);
    }
    return ResponseTimestamper.ok(project).build();
  }

  @PUT
  @Path("{projectKey}")
  public Response updateProject(@PathParam("projectKey") String projectKey, @Valid Project project,
      @Context Request req, @Context SecurityContext securityContext) {

    log.info("Updating project {} with {}", projectKey, project);
    if(isOmnipotentUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    ResponseTimestamper.evaluate(req, project); // FIXME...

    // update project use morphia query
    UpdateOperations<Project> ops =
        projects.datastore().createUpdateOperations(Project.class).set("name", project.getName());
    Query<Project> updateQuery = projects.datastore().createQuery(Project.class).field("key").equal(projectKey);

    projects.datastore().update(updateQuery, ops);

    return ResponseTimestamper.ok(project).build();
  }

  @GET
  @Path("{projectKey}/releases")
  public Response getReleases(@PathParam("projectKey") String projectKey, @Context SecurityContext securityContext) {

    log.debug("Getting releases for project: {}", projectKey);
    if(hasSpecificProjectPrivilege(securityContext, projectKey) == false) {
      return unauthorizedResponse();
    }

    Project project = projects.where(QProject.project.key.eq(projectKey)).uniqueResult();
    if(project == null) {
      return noSuchEntityResponse(projectKey);
    }
    return Response.ok(projects.getReleases(project)).build();
  }
}
