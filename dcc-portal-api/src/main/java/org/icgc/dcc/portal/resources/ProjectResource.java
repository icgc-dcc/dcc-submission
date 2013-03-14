/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.resources;

import com.google.inject.Inject;
import com.wordnik.swagger.annotations.*;
import com.yammer.dropwizard.jersey.params.IntParam;
import com.yammer.metrics.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpStatus;
import org.icgc.dcc.portal.repositories.ProjectRepository;
import org.icgc.dcc.portal.request.RequestSearchQuery;
import org.icgc.dcc.portal.responses.ErrorResponse;
import org.icgc.dcc.portal.results.FindAllResults;
import org.icgc.dcc.portal.results.FindResults;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/projects")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api(value = "/projects", description = "Operations about projects")
@Slf4j
public class ProjectResource {

  private static final String DEFAULT_SORT = "project_name";

  private static final String DEFAULT_ORDER = "asc";

  private final ProjectRepository store;

  @Inject
  public ProjectResource(ProjectRepository store) {
    this.store = store;
  }

  @GET
  @Timed
  @ApiOperation(value = "Retrieves a list of projects")
  public final Response query(
      @ApiParam(value = "Start index of results", required = false) @QueryParam("from") @DefaultValue("1") IntParam from,
      @ApiParam(value = "Number of results returned", allowableValues = "range[1,100]", required = false) @QueryParam("size") @DefaultValue("10") IntParam size,
      @ApiParam(value = "Column to sort results on", defaultValue = DEFAULT_SORT, required = false) @QueryParam("sort") String sort,
      @ApiParam(value = "Order to sort the column", defaultValue = DEFAULT_ORDER, allowableValues = "asc,desc", required = false) @QueryParam("order") String order,
      @ApiParam(value = "Filter the search results", required = false) @QueryParam("filters") String filters,
      @ApiParam(value = "Select fields returned", required = false) @QueryParam("fields") String fields) {
    String s = sort != null ? sort : DEFAULT_SORT;
    String o = order != null ? order : DEFAULT_ORDER;

    RequestSearchQuery requestSearchQuery = new RequestSearchQuery(filters, fields, from.get(), size.get(), s, o);

    FindAllResults results = store.findAll(requestSearchQuery);

    return Response.ok().entity(results).build();
  }

  @Path("/{id}")
  @GET
  @Timed
  // @CacheControl(immutable = true)
  // @ResourceFilters(GetNotFoundResourceFilter.class)
  @ApiOperation(value = "Find a project by id", notes = "If a project does not exist with the specified id an error will be returned")
  @ApiErrors(value = {@ApiError(code = HttpStatus.NOT_FOUND_404, reason = "Project not found")})
  public final Response get(@ApiParam(value = "Project ID") @PathParam("id") String id) throws IOException {
    FindResults response = store.find(id);

    if (response.getFields() == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse(Response.Status.NOT_FOUND, "Project " + id + " not found.")).build();
    }

    return Response.ok().entity(response).build();
  }
}
