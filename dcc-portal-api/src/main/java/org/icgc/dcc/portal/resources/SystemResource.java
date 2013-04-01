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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import org.icgc.dcc.portal.health.ElasticSearchHealthCheck;
import org.icgc.dcc.portal.health.MongoHealthCheck;

@Path("/system")
@Produces(APPLICATION_JSON)
@Consumes(APPLICATION_JSON)
@Api(value = "/system", description = "Operations about system status")
public class SystemResource {

  private final ElasticSearchHealthCheck esHealth;

  private final MongoHealthCheck mongoHealthCheck;

  @Inject
  public SystemResource(ElasticSearchHealthCheck esHealth, MongoHealthCheck mongoHealthCheck) {
    this.esHealth = esHealth;
    this.mongoHealthCheck = mongoHealthCheck;
  }

  @GET
  @ApiOperation(value = "Retrieves system status information")
  public final Response status() {
    Map<String, Object> results = new HashMap<String, Object>();
    results.put("elastic_search", esHealth.execute());
    results.put("mongo", mongoHealthCheck.execute());

    return Response.ok().entity(results).build();
  }
}
