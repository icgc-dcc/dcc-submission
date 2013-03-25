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
import org.icgc.dcc.portal.repositories.GeneProjectRepository;
import org.icgc.dcc.portal.repositories.GeneRepository;
import org.icgc.dcc.portal.request.RequestSearchQuery;
import org.icgc.dcc.portal.responses.ErrorResponse;
import org.icgc.dcc.portal.results.FindAllResults;
import org.icgc.dcc.portal.results.FindResults;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/genes")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api(value = "/genes", description = "Operations about genes")
@Slf4j
public class GeneResource {

  private static final String DEFAULT_SORT = "_score";

  private static final String DEFAULT_ORDER = "desc";

  private final GeneRepository store;

  private final GeneProjectRepository gp;

  @Inject
  public GeneResource(GeneRepository store, GeneProjectRepository gp) {
    this.store = store;
    this.gp = gp;
  }

  @GET
  @Timed
  @ApiOperation(value = "Retrieves a list of genes")
  public final Response findAll(
      @ApiParam(value = "Start index of results", required = false) @QueryParam("from") @DefaultValue("1") IntParam from,
      @ApiParam(value = "Number of results returned", allowableValues = "range[1,100]", required = false) @QueryParam("size") @DefaultValue("10") IntParam size,
      @ApiParam(value = "Column to sort results on", defaultValue = DEFAULT_SORT, required = false) @QueryParam("sort") String sort,
      @ApiParam(value = "Order to sort the column", defaultValue = DEFAULT_ORDER, allowableValues = "asc,desc", required = false) @QueryParam("order") String order,
      @ApiParam(value = "Filter the search results", required = false) @QueryParam("filters") String filters,
      @ApiParam(value = "Select fields returned", required = false) @QueryParam("fields") String fields) {
    String s = sort != null ? sort : DEFAULT_SORT;
    String o = order != null ? order : DEFAULT_ORDER;

    RequestSearchQuery requestSearchQuery =
        RequestSearchQuery.builder().filters(filters).fields(fields).from(from.get()).size(size.get()).sort(s).order(o)
            .build();

    FindAllResults results = store.findAll(requestSearchQuery);

    return Response.ok().entity(results).build();
  }

  @Path("/{id}")
  @GET
  @Timed
  @ApiOperation(value = "Find a gene by id", notes = "If a gene does not exist with the specified id an error will be returned")
  @ApiErrors(value = {@ApiError(code = HttpStatus.NOT_FOUND_404, reason = "Gene not found")})
  public final Response find(@ApiParam(value = "Gene ID") @PathParam("id") String id) {
    FindResults results = store.find(id);

    if (results.getFields() == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse(Response.Status.NOT_FOUND, "Gene " + id + " not found.")).build();
    }

    return Response.ok().entity(results).build();
  }

  @Path("/projects")
  @GET
  @Timed
  @ApiOperation(value = "Find a gene data by project")
  public final Response findByProjects(
      @ApiParam(value = "Start index of results", required = false) @QueryParam("from") @DefaultValue("1") IntParam from,
      @ApiParam(value = "Number of results returned", allowableValues = "range[1,100]", required = false) @QueryParam("size") @DefaultValue("10") IntParam size,
      @ApiParam(value = "Column to sort results on", defaultValue = DEFAULT_SORT, required = false) @QueryParam("sort") String sort,
      @ApiParam(value = "Order to sort the column", defaultValue = DEFAULT_ORDER, allowableValues = "asc,desc", required = false) @QueryParam("order") String order) {
    String s = sort != null ? sort : DEFAULT_SORT;
    String o = order != null ? order : DEFAULT_ORDER;

    RequestSearchQuery requestSearchQuery =
        RequestSearchQuery.builder().from(from.get()).size(size.get()).sort(s).order(o)
            .build();

    FindAllResults results = gp.findAll(requestSearchQuery);

    return Response.ok().entity(results).build();
  }

  @Path("/projects/{id}")
  @GET
  @Timed
  @ApiOperation(value = "Find a gene data by project")
  public final Response findByProject(@ApiParam(value = "Project ID") @PathParam("id") String id,
                                      @ApiParam(value = "Start index of results", required = false) @QueryParam("from") @DefaultValue("1") IntParam from,
                                      @ApiParam(value = "Number of results returned", allowableValues = "range[1,100]", required = false) @QueryParam("size") @DefaultValue("10") IntParam size,
                                      @ApiParam(value = "Column to sort results on", defaultValue = DEFAULT_SORT, required = false) @QueryParam("sort") String sort,
                                      @ApiParam(value = "Order to sort the column", defaultValue = DEFAULT_ORDER, allowableValues = "asc,desc", required = false) @QueryParam("order") String order) {
    String s = sort != null ? sort : DEFAULT_SORT;
    String o = order != null ? order : DEFAULT_ORDER;
    String filters = "{'project': {'project_key': '" + id + "'}}";

    RequestSearchQuery requestSearchQuery =
        RequestSearchQuery.builder().filters(filters).from(from.get()).size(size.get()).sort(s).order(o)
            .build();

    FindAllResults results = gp.findAll(requestSearchQuery);

    return Response.ok().entity(results).build();
  }
}
