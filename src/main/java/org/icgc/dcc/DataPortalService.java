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

import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.StringUtils.repeat;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;

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

	private static final String PACKAGE = DataPortalService.class.getPackage().getName();
	private static final String APPLICATION_NAME = "icgc-data-portal-api";
	private static String[] args;

	public static void main(String... args) throws Exception {
		DataPortalService.args = args;
		new DataPortalService().run(new String[] { "server", "settings.yml" });
	}

	@Override
	public final void initialize(Bootstrap<DataPortalConfiguration> bootstrap) {
		bootstrap.setName(APPLICATION_NAME);
		bootstrap.addBundle(new SwaggerBundle());
		bootstrap.addBundle(GuiceBundle.newBuilder().addModule(new DataPortalModule()).enableAutoConfig(PACKAGE).build());
		bootstrap.addBundle(new RedirectBundle(ImmutableMap.<String, String> builder().put("/", "/docs/").put("/docs", "/docs/").build()));
	}

	@Override
	public final void run(DataPortalConfiguration configuration, Environment environment) throws Exception {
		logInfo(args);
	}

	private static void logInfo(String[] args) {
		log.info("");
		log.info(repeat("-", 80));
		log.info("  {}", APPLICATION_NAME.toUpperCase() + " " + getVersion());
		log.info("    > {}", formatArguments(args));
		log.info(repeat("-", 80));
		log.info("");
	}

	private static String getVersion() {
		String implementationVersion = DataPortalService.class.getPackage().getImplementationVersion();
		return implementationVersion == null ? "" : "v" + implementationVersion;
	}

	private static String getJarName() {
		String jarPath = DataPortalService.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		File jarFile = new File(jarPath);

		return jarFile.getName();
	}

	private static String formatArguments(String[] args) {
		RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
		List<String> inputArguments = runtime.getInputArguments();

		return "java " + join(inputArguments, ' ') + " -jar " + getJarName() + " " + join(args, ' ');
	}

}
