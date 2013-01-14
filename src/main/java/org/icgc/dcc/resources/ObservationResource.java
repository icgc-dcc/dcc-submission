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

package org.icgc.dcc.resources;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.http.HttpStatus;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.metrics.annotation.Timed;

import org.icgc.dcc.core.Types;
import org.icgc.dcc.repositories.SearchRepository;
import org.icgc.dcc.responses.ManyResponse;
import org.icgc.dcc.responses.SingleResponse;
import org.icgc.dcc.search.SearchQuery;

@Path("/observations")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api(value = "/observations", description = "Operations about observations")
@Slf4j
public class ObservationResource {

  private final SearchRepository store;

  @Context
  private HttpServletRequest httpServletRequest;

  @Inject
  public ObservationResource(SearchRepository searchRepository) {
    this.store = searchRepository.withType(Types.OBSERVATIONS);
  }

  @GET
  @Timed
  @ApiOperation(value = "Retrieves a list of observations")
  public final Response getAll(
      @ApiParam(value = "Start index of results", required = false) @QueryParam("from") @DefaultValue("1") int from,
      @ApiParam(value = "Number of results returned", allowableValues = "range[1,100]", required = false) @QueryParam("size") @DefaultValue("10") int size,
      @ApiParam(value = "Column to sort results on", required = false) @QueryParam("sort") String sort,
      @ApiParam(value = "Order to sort the column", allowableValues = "asc, desc", required = false) @QueryParam("order") String order) {
    SearchQuery searchQuery = new SearchQuery(from, size, sort, order);
    ManyResponse response = new ManyResponse(store.getAll(searchQuery), httpServletRequest, searchQuery);

    return Response.ok().entity(response).build();
  }

  @POST
  @Timed
  @ApiOperation(value = "Retrieves a filtered list of observations")
  public final Response filteredGetAll(@Valid SearchQuery searchQuery) {
    // TODO This is broken
    ManyResponse response = new ManyResponse(store.getAll(searchQuery), httpServletRequest, searchQuery);

    return Response.ok().entity(response).build();
  }

  @Path("/{id}")
  @GET
  @Timed
  @ApiOperation(value = "Find a observation by id", notes = "If a observation does not exist with the specified id an error will be returned")
  @ApiErrors(value = {@ApiError(code = HttpStatus.BAD_REQUEST_400, reason = "Invalid ID supplied"),
      @ApiError(code = HttpStatus.NOT_FOUND_404, reason = "Observation not found")})
  public final Response getOne(
      @ApiParam(value = "ID of observation that needs to be fetched") @PathParam("id") String id) throws IOException {
    SingleResponse response = new SingleResponse(store.getOne(id), httpServletRequest);

    return Response.ok().entity(response).build();
  }

}
