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

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.metrics.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.icgc.dcc.portal.repositories.ISearchRepository;
import org.icgc.dcc.portal.responses.GetManyResponse;
import org.icgc.dcc.portal.search.SearchQuery;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/search")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api(value = "/search", description = "Text search")
@Slf4j
public class SearchResource {

  private final ISearchRepository store;

  @Context
  private HttpServletRequest httpServletRequest;

  @Inject
  public SearchResource(ISearchRepository searchRepository) {
    this.store = searchRepository;
  }

  @GET
  @Timed
  @ApiOperation(value = "Search")
  public final Response search(
      @ApiParam(value = "Term to search") @QueryParam("text") @DefaultValue("*") String text,
      @ApiParam(value = "Start index of results", required = false) @QueryParam("from") @DefaultValue("1") int from,
      @ApiParam(value = "Number of results returned", allowableValues = "range[1,100]", required = false) @QueryParam("size") @DefaultValue("10") int size) {
    SearchQuery searchQuery = new SearchQuery(from, size);
    GetManyResponse response = new GetManyResponse(store.search(text, from, size), searchQuery);

    return Response.ok().entity(response).build();
  }
}
