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

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;

import org.eclipse.jetty.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.jsonschema.JsonSchema;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiError;
import com.wordnik.swagger.annotations.ApiErrors;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.yammer.metrics.annotation.Timed;

import org.icgc.dcc.dao.GeneDao;

@Path("/genes")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/genes", description = "Operations about genes")
public class GeneResource {

  private final GeneDao geneDao;

  @Inject
  public GeneResource(GeneDao geneDao) {
    this.geneDao = geneDao;
  }

 	@GET
	@Timed
	@ApiOperation("Retrieve a list of genes")
	public final Response getAll() {
		return Response.ok().entity(geneDao.getAll()).build();
	}

	@Path("/_schema")
	@GET
	@ApiOperation("Returns JSON Schema of Resource")
	public final Response getSchema() throws JsonMappingException {
		JsonSchema s = geneDao.getSchema();
		//ResourceFactory
		return Response.ok(s).build();
	}

	@Path("/{id}")
	@GET
	@Timed
	@ApiOperation(value = "Find a gene by id", notes = "If a gene does not exist with the specified id an error will be returned", responseClass = "org.icgc.dcc.core.Gene")
	@ApiErrors(value = {@ApiError(code = HttpStatus.BAD_REQUEST_400, reason = "Invalid ID supplied"),
			@ApiError(code = HttpStatus.NOT_FOUND_404, reason = "Gene not found")})
	public final Response getOne(
			@ApiParam(value = "id of gene that needs to be fetched") @PathParam("id") String id) throws IOException {
		return Response.ok(geneDao.getOne(id)).build();
	}
}
