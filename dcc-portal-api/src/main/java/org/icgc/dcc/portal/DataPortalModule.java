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

package org.icgc.dcc.portal;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.icgc.dcc.portal.configurations.ElasticSearchConfiguration;
import org.icgc.dcc.portal.configurations.MongoDbConfiguration;
import org.jongo.Jongo;

import java.net.UnknownHostException;

public class DataPortalModule extends AbstractModule {

  @Provides
  @Singleton
  public final Mongo mongo(DataPortalConfiguration dpc) throws UnknownHostException {
    MongoDbConfiguration configuration = dpc.getMongo();
    // Mongo is thread-safe so @Singleton is appropriate
    return new Mongo(new MongoURI(configuration.getUri()));
  }

  @Provides
  @Singleton
  public final Jongo jongo(DataPortalConfiguration dpc, Mongo mongo) {
    MongoDbConfiguration configuration = dpc.getMongo();
    DB db = mongo.getDB(configuration.getDb());
    return new Jongo(db);
  }

  @Provides
  @Singleton
  public final Client elasticClient(DataPortalConfiguration dpc) {
    // TransportClient is thread-safe so @Singleton is appropriate
    ElasticSearchConfiguration configuration = dpc.getElastic();
    return new TransportClient().addTransportAddress(new InetSocketTransportAddress(configuration.getHost(),
        configuration.getPort()));
  }

  @Override
  protected final void configure() {
    // Guice bindings go here
  }
}
