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

import com.mongodb.Mongo;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.Client;
import org.icgc.dcc.dao.GeneDao;
import org.icgc.dcc.health.ElasticSearchHealthCheck;
import org.icgc.dcc.health.MongoHealthCheck;
import org.icgc.dcc.managers.ElasticSearchClientManager;
import org.icgc.dcc.managers.MongoClientManager;
import org.icgc.dcc.resources.GeneResource;
import org.icgc.dcc.utils.ElasticSearchHelper;
import org.icgc.dcc.utils.MongoHelper;

@Slf4j
public class DataPortalService extends Service<DataPortalConfiguration> {
    private static final String APPLICATION_NAME = "data-portal";

    public static void main(String[] args) throws Exception {
        new DataPortalService().run(new String[]{"server", "settings.yml"});
    }

    @Override
    public final void initialize(Bootstrap<DataPortalConfiguration> bootstrap) {
        bootstrap.setName(APPLICATION_NAME);
    }

    @Override
    public final void run(DataPortalConfiguration configuration,
                          Environment environment) {

        Mongo mongo = MongoHelper.getMongoClient();
        Client es = ElasticSearchHelper.getESClient();

        environment.manage(new ElasticSearchClientManager(es));
        environment.manage(new MongoClientManager(mongo));

        environment.addHealthCheck(new MongoHealthCheck(mongo));
        environment.addHealthCheck(new ElasticSearchHealthCheck(es));

        environment.addResource(new GeneResource(new GeneDao(es)));
    }
}
