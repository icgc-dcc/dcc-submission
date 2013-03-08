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

import com.wordnik.swagger.annotations.*;
import com.yammer.dropwizard.jersey.caching.CacheControl;
import com.yammer.dropwizard.jersey.params.IntParam;
import com.yammer.metrics.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpStatus;
import org.icgc.dcc.portal.repositories.IGeneRepository;
import org.icgc.dcc.portal.responses.GetManyResponse;
import org.icgc.dcc.portal.responses.GetOneResponse;
import org.icgc.dcc.portal.search.GeneSearchQuery;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/genes")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api(value = "/genes", description = "Operations about genes")
@Slf4j
public class GeneResource {

  @Context
  private HttpHeaders headers;

  private final IGeneRepository store;

  @Inject
  public GeneResource(IGeneRepository store) {
    this.store = store;
  }

  @GET
  @Timed
  // @CacheControl(maxAge = 6, maxAgeUnit = TimeUnit.HOURS)
  @ApiOperation(value = "Retrieves a list of genes")
  public final Response getAll(
      @Context Request request,
      @ApiParam(value = "Start index of results", required = false) @QueryParam("from") @DefaultValue("1") IntParam from,
      @ApiParam(value = "Number of results returned", allowableValues = "range[1,100]", required = false) @QueryParam("size") @DefaultValue("10") IntParam size,
      @ApiParam(value = "Column to sort results on", defaultValue = "start", required = false) @QueryParam("sort") String sort,
      @ApiParam(value = "Order to sort the column", defaultValue = "asc", allowableValues = "asc,desc", required = false) @QueryParam("order") String order,
      @ApiParam(value = "Filter the search results", required = false) @QueryParam("filters") String filters,
      @ApiParam(value = "Select fields returned", required = false) @QueryParam("fields") String fields) {
    GeneSearchQuery searchQuery = new GeneSearchQuery(filters, fields, from.get(), size.get(), sort, order);

    GetManyResponse results = new GetManyResponse(store.getAll(searchQuery), searchQuery);
    /*
     * EntityTag etag = new
     * EntityTag(Hashing.murmur3_128().hashString(results.toString()).toString());
     * Response.ResponseBuilder rb = request.evaluatePreconditions(etag); if (rb != null) { return
     * rb.build(); } /* if (oldEtag != null && oldEtag.replaceAll("\"", "").equals(etag)) { return
     * Response.notModified().header("X-ICGC-Version", "1") //
     * .contentLocation(URI.create(oldEtag.getRequestURI())) .build(); }
     */
    return Response.ok().entity(results).build();
    // .header("X-ICGC-Version", "1")
    // .contentLocation(URI.create(httpServletRequest.getRequestURI()))
    // .tag(etag).entity(results).build();
  }

  @Path("/{id}")
  @GET
  @Timed
  @CacheControl(immutable = true)
  @ApiOperation(value = "Find a gene by id", notes = "If a gene does not exist with the specified id an error will be returned")
  @ApiErrors(value = {@ApiError(code = HttpStatus.BAD_REQUEST_400, reason = "Invalid ID supplied"),
      @ApiError(code = HttpStatus.NOT_FOUND_404, reason = "Gene not found")})
  public final Response getOne(@ApiParam(value = "ID of gene that needs to be fetched") @PathParam("id") String id)
      throws IOException {
    GetOneResponse response = new GetOneResponse(store.getOne(id));

    return Response.ok().entity(response).build();
  }
}
