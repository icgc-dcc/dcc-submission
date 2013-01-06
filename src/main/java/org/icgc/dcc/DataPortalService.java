/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc;

import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.bundles.SwaggerBundle;

import com.bazaarvoice.dropwizard.redirect.RedirectBundle;
import com.google.common.collect.ImmutableMap;
import com.hubspot.dropwizard.guice.GuiceBundle;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

@Slf4j
public class DataPortalService extends Service<DataPortalConfiguration> {

	private static final String APPLICATION_NAME = "icgc-data-portal-api";

	public static void main(String[] args) throws Exception {
		new DataPortalService().run(args);
	}

	@Override
	public final void initialize(Bootstrap<DataPortalConfiguration> bootstrap) {
		bootstrap.setName(APPLICATION_NAME);
		bootstrap.addBundle(new SwaggerBundle());
		bootstrap.addBundle(GuiceBundle.newBuilder().addModule(new DataPortalModule()).enableAutoConfig(getClass().getPackage().getName()).build());
		bootstrap.addBundle(new RedirectBundle(ImmutableMap.<String, String> builder().put("/", "/docs/").put("/api-docs/*", "/api-docs.json/*").build()));
	}

	@Override
	public void run(DataPortalConfiguration configuration, Environment environment) throws Exception {
		log.info("Running service...");
	}

}
