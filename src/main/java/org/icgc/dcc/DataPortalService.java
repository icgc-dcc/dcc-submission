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

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import org.icgc.dcc.core.Gene;
import org.icgc.dcc.health.MongoHealthCheck;
import org.icgc.dcc.resources.GeneResource;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;

public class DataPortalService extends Service<DataPortalConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataPortalService.class);

    public static void main(String[] args) throws Exception {
        new DataPortalService().run(new String[]{"server", "settings.yml"});
    }

    @Override
    public void initialize(Bootstrap<DataPortalConfiguration> bootstrap) {
        bootstrap.setName("data-portal");
    }

    @Override
    public void run(DataPortalConfiguration configuration,
                    Environment environment) {

        Mongo mongo = null;
        DB db = null;
        try {
            mongo = new Mongo();
            db = mongo.getDB("data-portal-local");
        } catch (UnknownHostException e) {
            LOGGER.error("MongoDB Connection Error", e);
        }

        MongoManaged mongoManaged = new MongoManaged(mongo);

        Jongo jongo = new Jongo(db);
        MongoCollection genes = jongo.getCollection("genes");
        genes.insert("{name: 'Joe', age: 18}");
        Gene gene = genes.findOne().as(Gene.class);
        LOGGER.info(gene.getName());

        environment.addHealthCheck(new MongoHealthCheck(mongo));

        environment.manage(mongoManaged);
        environment.addResource(new GeneResource());
    }
}
