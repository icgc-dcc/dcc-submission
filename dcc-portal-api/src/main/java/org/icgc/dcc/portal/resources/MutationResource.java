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

import static com.google.common.base.Objects.firstNonNull;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.IOException;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.jetty.http.HttpStatus;
import org.hibernate.validator.constraints.NotEmpty;

import com.google.inject.Inject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.dropwizard.jersey.params.IntParam;
import com.yammer.metrics.annotation.Timed;

import org.icgc.dcc.portal.repositories.MutationRepository;
import org.icgc.dcc.portal.request.RequestSearchQuery;
import org.icgc.dcc.portal.responses.ErrorResponse;
import org.icgc.dcc.portal.results.FindAllResults;
import org.icgc.dcc.portal.results.FindResults;

@Path("/mutations")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api(value = "/mutations", description = "Operations about mutations")
public class MutationResource {

  private static final String DEFAULT_SORT = "_score";

  private static final String DEFAULT_ORDER = "desc";

  private final MutationRepository store;

  @Inject
  public MutationResource(MutationRepository store) {
    this.store = store;
  }

  @GET
  @Timed
  @ApiOperation(value = "Retrieves a list of mutations")
  public final FindAllResults findAll(
      @ApiParam(value = "Start index of results", required = false) @QueryParam("from") @DefaultValue("1")//
      IntParam from, //
      @ApiParam(value = "Number of results returned", allowableValues = "range[1,100]", required = false) @QueryParam("size") @DefaultValue("10")//
      IntParam size, //
      @ApiParam(value = "Column to sort results on", defaultValue = DEFAULT_SORT, required = false) @QueryParam("sort")//
      String sort, //
      @ApiParam(value = "Order to sort the column", defaultValue = DEFAULT_ORDER, allowableValues = "asc,desc", required = false) @QueryParam("order")//
      String order, //
      @ApiParam(value = "Filter the search results", required = false) @QueryParam("filters")//
      String filters, //
      @ApiParam(value = "Select fields returned", required = false) @QueryParam("fields")//
      String fields) {
    String s = firstNonNull(sort, DEFAULT_SORT);
    String o = firstNonNull(order, DEFAULT_ORDER);

    RequestSearchQuery requestSearchQuery = RequestSearchQuery.builder()//
        .filters(filters)//
        .fields(fields)//
        .from(from.get())//
        .size(size.get())//
        .sort(s)//
        .order(o)//
        .build();

    return store.findAll(requestSearchQuery);
  }

  @Path("/{id}")
  @GET
  @Timed
  @ApiOperation(value = "Find a gene by id", notes = "If a mutation does not exist with the specified id an error will be returned")
  @ApiErrors(value = {@ApiError(code = HttpStatus.NOT_FOUND_404, reason = "Mutation not found")})
  public final Response find(//
      @ApiParam(value = "Mutation ID") @Valid @NotEmpty @PathParam("id")//
      String id) throws IOException {
    FindResults results = store.find(id);

    if (results.getFields() == null) {
      return status(NOT_FOUND).entity(new ErrorResponse(NOT_FOUND, "Mutation " + id + " not found.")).build();
    }

    return ok().entity(results).build();
  }

}
