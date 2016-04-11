/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static org.icgc.dcc.submission.web.util.Authorizations.isSuperUser;

import java.io.IOException;
import java.io.InputStream;

import javax.validation.Valid;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.core.model.User;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.web.util.Responses;
import org.mongodb.morphia.Datastore;

import com.google.inject.Inject;

import lombok.val;

/**
 * TODO: discard class: DCC-819 (was originally created in the context of DCC-135)
 * <p>
 * The integration test currently relies on it
 */
@Path("seed")
public class SeedResource {

  @Context
  private HttpHeaders requestHeaders;

  @Inject
  private Datastore datastore;

  @Inject
  private DccFileSystem fileSystem;

  @POST
  @Path("users")
  public Response seedUsers(

      @Context SecurityContext securityContext,

      @Valid User[] users,

      @QueryParam("delete") @DefaultValue("false") boolean delete

      )
  {
    if (isSuperUser(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    if (delete) {
      datastore.getCollection(User.class).drop();
    }
    datastore.save(users);
    return Response.status(Status.CREATED).build();
  }

  @POST
  @Path("projects")
  public Response seedProjects(

      @Context SecurityContext securityContext,

      @Valid Project[] projects,

      @QueryParam("delete") @DefaultValue("false") boolean delete

      )
  {
    if (isSuperUser(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    if (delete) {
      datastore.getCollection(Project.class).drop();
    }
    datastore.save(projects);
    return Response.status(Status.CREATED).build();
  }

  @POST
  @Path("releases")
  public Response seedReleases(

      @Context SecurityContext securityContext,

      @Valid Release[] releases,

      @QueryParam("delete") @DefaultValue("false") boolean delete

      )
  {
    if (isSuperUser(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    if (delete) {
      datastore.getCollection(Release.class).drop();
    }
    datastore.save(releases);
    return Response.status(Status.CREATED).build();
  }

  @POST
  @Path("dictionaries")
  public Response seedDictionaries(

      @Context SecurityContext securityContext,

      @Valid Dictionary[] dictionaries,

      @QueryParam("delete") @DefaultValue("false") boolean delete

      )
  {
    if (isSuperUser(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    if (delete) {
      datastore.getCollection(Dictionary.class).drop();
    }

    datastore.save(dictionaries);

    return Response.status(Status.CREATED).build();
  }

  @POST
  @Path("codelists")
  public Response seedCodeLists(

      @Context SecurityContext securityContext,

      @Valid CodeList[] codelists,

      @QueryParam("delete") @DefaultValue("false") boolean delete

      )
  {
    if (isSuperUser(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    if (delete) {
      datastore.getCollection(CodeList.class).drop();
    }

    datastore.save(codelists);

    return Response.status(Status.CREATED).build();
  }

  @POST
  @Path("fs/{filepath: .*}")
  public Response seedFileSystem(

      @Context SecurityContext securityContext,

      @PathParam("filepath") String filename,

      InputStream fileContents

      )
  {
    if (isSuperUser(securityContext) == false) {
      return Responses.unauthorizedResponse();
    }

    FileSystem fs = this.fileSystem.getFileSystem();
    val destinationPath = new org.apache.hadoop.fs.Path(fileSystem.getRootStringPath() + "/" + filename);

    try {
      val fileDestination = fs.create(destinationPath);

      IOUtils.copy(fileContents, fileDestination);
      fileDestination.flush();
      fileDestination.close();
    } catch (IOException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    return Response.status(Status.CREATED).build();
  }

}
