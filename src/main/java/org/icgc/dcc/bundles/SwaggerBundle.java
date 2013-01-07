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

package org.icgc.dcc.bundles;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.jaxrs.JaxrsApiReader;
import com.wordnik.swagger.jaxrs.listing.ApiListing;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;

public class SwaggerBundle extends AssetsBundle {

	private static final String EMPTY_FORMAT = "";

	private static final String RESOURCE_PATH = "/swagger-ui/";

	private static final String URI_PATH = "/docs/";

	public SwaggerBundle() {
		super(RESOURCE_PATH, URI_PATH, "index.html");
	}

	@Override
	public final void initialize(Bootstrap<?> bootstrap) {
		super.initialize(bootstrap);

		// Set the Swagger suffix to an empty string before Swagger warms up
		JaxrsApiReader.setFormatString(EMPTY_FORMAT);
	}

	/**
	 * Required to remove the Swagger <code>.{format}</code> extension from Swagger resources:
	 * <ul>
	 * <li><code>/api-docs</code></li>
	 * <li><code>/api-docs/{route}</code></li>
	 * </ul>
	 * 
	 * @author btiernay
	 */
	@Path("/api-docs")
	@Api("/api-docs")
	@Produces(MediaType.APPLICATION_JSON)
	public static class ApiListingResourceJSON extends ApiListing {
		// Empty
	}

}
