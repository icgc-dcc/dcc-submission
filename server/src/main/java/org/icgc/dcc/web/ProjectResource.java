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

import static com.google.common.base.Preconditions.checkArgument;

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

import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.QProject;
import org.icgc.dcc.shiro.AuthorizationPrivileges;
import org.icgc.dcc.shiro.ShiroSecurityContext;

import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mongodb.MongoException.DuplicateKey;

@Path("projects")
public class ProjectResource {

  @Inject
  private ProjectService projects;

  @GET
  public Response getProjects(@Context SecurityContext securityContext) {
    List<Project> projectlist = projects.getProjects(((ShiroSecurityContext) securityContext).getSubject());
    if(projectlist == null) {
      projectlist = Lists.newArrayList();
    }
    return Response.ok(projectlist).build();
  }

  @POST
  @Consumes("application/json")
  public Response addProject(@Valid Project project) {
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
  public Response getIt(@PathParam("projectKey") String projectKey, @Context SecurityContext securityContext) {
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.projectViewPrivilege(projectKey)) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    Project project = projects.where(QProject.project.key.eq(projectKey)).uniqueResult();
    if(project == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, projectKey)).build();
    }
    return ResponseTimestamper.ok(project).build();
  }

  @PUT
  @Path("{projectKey}")
  public Response updateProject(@PathParam("projectKey") String projectKey, @Valid Project project,
      @Context Request req, @Context SecurityContext securityContext) {
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.projectViewPrivilege(projectKey)) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    ResponseTimestamper.evaluate(req, project);

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
    if(((ShiroSecurityContext) securityContext).getSubject().isPermitted(
        AuthorizationPrivileges.projectViewPrivilege(projectKey)) == false) {
      return Response.status(Status.UNAUTHORIZED).entity(new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED))
          .build();
    }
    Project project = projects.where(QProject.project.key.eq(projectKey)).uniqueResult();
    if(project == null) {
      return Response.status(Status.NOT_FOUND)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, projectKey)).build();
    }
    return Response.ok(projects.getReleases(project)).build();
  }
}
