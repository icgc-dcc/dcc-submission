/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.test.es;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import lombok.extern.slf4j.Slf4j;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

@Slf4j
public class EmbeddedElasticSearch implements TestRule {

  private Node node;

  private Client client;

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        log.info("Starting embedded ElasticSearch...");
        start();
        log.info("Embedded ElasticSearch started");

        base.evaluate();

        log.info("Stopping embedded ElasticSearch...");
        stop();
        log.info("Embedded ElasticSearch stopped");
      }
    };
  }

  public int getPort() {
    return 9300;
  }

  public Client getClient() {
    return client;
  }

  private void start() {
    // Build settings
    Settings settings = settingsBuilder()//
        // .put("node.local", true)//
        .put("node.name", "node-test")//
        .put("node.data", true)//
        .put("index.store.type", "memory")//
        .put("index.store.fs.memory.enabled", "true")//
        .put("index.number_of_shards", "1")//
        .put("index.number_of_replicas", "0")//
        .put("gateway.type", "none")//
        .put("path.data", "target/elasticsearch-test/data")//
        .put("path.work", "target/elasticsearch-test/work")//
        .put("path.logs", "target/elasticsearch-test/logs")//
        .put("cluster.routing.schedule", "50ms") //
        .build();

    node = NodeBuilder.nodeBuilder().settings(settings).node();
    client = node.client();

    // Wait for Yellow status
    client.admin().cluster() //
        .prepareHealth() //
        .setWaitForYellowStatus() //
        .setTimeout(TimeValue.timeValueMinutes(1)) //
        .execute() //
        .actionGet(); //
  }

  private void stop() {
    node.stop();
  }

}
