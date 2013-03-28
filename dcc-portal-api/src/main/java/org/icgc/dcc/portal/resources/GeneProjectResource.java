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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.dropwizard.jersey.params.IntParam;
import com.yammer.metrics.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.icgc.dcc.portal.repositories.GeneProjectRepository;
import org.icgc.dcc.portal.request.RequestSearchQuery;
import org.icgc.dcc.portal.results.FindAllResults;
import org.icgc.dcc.portal.results.FindResults;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/gp")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api(value = "/gp", description = "Operations about genes related to projects")
@Slf4j
public class GeneProjectResource {

  private static final String DEFAULT_SORT = "_score";

  private static final String DEFAULT_ORDER = "desc";

  private final GeneProjectRepository gp;

  @Inject
  public GeneProjectResource(GeneProjectRepository gp) {
    this.gp = gp;
  }

  @GET
  @Timed
  @ApiOperation(value = "Find a gene data by project")
  public final Response findAll(
      //@ApiParam(value = "Start index of results", required = false) @QueryParam("from") @DefaultValue("1") IntParam from,
      //@ApiParam(value = "Number of results returned", allowableValues = "range[1,100]", required = false) @QueryParam("size") @DefaultValue("10") IntParam size,
      //@ApiParam(value = "Column to sort results on", defaultValue = DEFAULT_SORT, required = false) @QueryParam("sort") String sort,
      //@ApiParam(value = "Order to sort the column", defaultValue = DEFAULT_ORDER, allowableValues = "asc,desc", required = false) @QueryParam("order") String order,
      //@ApiParam(value = "Select fields returned", required = false) @QueryParam("fields") String fields,
      @ApiParam(value = "Filter the search results", required = false) @QueryParam("filters") String filters
  ) {
    String s = "_score";
    String o = "desc";

    RequestSearchQuery requestSearchQuery =
        RequestSearchQuery.builder().filters(filters)
            .from(0).size(50)
            .sort(s).order(o)
            .build();

    FindAllResults results = gp.findAll(requestSearchQuery);

    FindAllResults filteredResults;

    return Response.ok().entity(results).build();
  }

  @Path("/project/{id}")
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
    String filters = "{'project': {'_project_id': '" + id + "'}}";

    RequestSearchQuery requestSearchQuery =
        RequestSearchQuery.builder().filters(filters).from(from.get()).size(size.get()).sort(s).order(o)
            .build();

    FindAllResults results = gp.findAll(requestSearchQuery);

    return Response.ok().entity(results).build();
  }

  @Path("/genes/{id}")
  @GET
  @Timed
  @ApiOperation(value = "Find a gene data by project")
  public final Response findByGene(@ApiParam(value = "Project ID") @PathParam("id") String id) {

    FindResults results = gp.find(id);

    return Response.ok().entity(results).build();
  }
}
