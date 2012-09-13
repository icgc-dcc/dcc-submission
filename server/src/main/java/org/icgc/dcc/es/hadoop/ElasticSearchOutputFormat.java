/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.es.hadoop;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.Progressable;
import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.indices.IndexAlreadyExistsException;

/**
 * 
 */
public class ElasticSearchOutputFormat implements OutputFormat<String, String> {

  static Log log = LogFactory.getLog(ElasticSearchOutputFormat.class);

  private static class ElasticSearchRecordWriter implements RecordWriter<String, String> {

    private final Progressable progress;

    private final Client client;

    private final int bulkSize;

    private final String indexName;

    private final String type;

    private boolean started = false;

    private BulkRequestBuilder currentRequest;

    private ElasticSearchRecordWriter(JobConf conf, Progressable progress) {
      ElasticSearchConfig config = new ElasticSearchConfig(conf);
      this.bulkSize = config.getBulkSize();
      this.indexName = config.getIndex();
      this.type = config.getType();

      this.progress = progress;
      TransportClient transportClient = new TransportClient();
      for(TransportAddress address : config.getTransportAddresses()) {
        transportClient.addTransportAddress(address);
      }
      this.client = transportClient;
      this.currentRequest = this.client.prepareBulk();
    }

    private void prepareIndex() {
      try {
        this.client.admin().indices().prepareCreate(indexName).execute().actionGet();
      } catch(IndexAlreadyExistsException e) {
        // ignore
      } catch(ElasticSearchException e) {
        throw e;
      }
    }

    @Override
    public void write(String key, String value) throws IOException {
      if(value == null || value.isEmpty()) {
        log.warn("empty document for key " + key);
      } else {
        currentRequest.add(new IndexRequestBuilder(this.client).setIndex(indexName).setType(type).setSource(value)
            .setId(key).request());
        sendRequestIfNecessary();
        progress.progress();
      }
    }

    @Override
    public void close(Reporter reporter) throws IOException {
      sendRequest();
      client.close();
    }

    private void sendRequestIfNecessary() throws IOException {
      if(currentRequest.numberOfActions() >= bulkSize) {
        sendRequest();
      }
    }

    private void sendRequest() throws IOException {
      if(currentRequest.numberOfActions() > 0) {
        try {
          ensureStarted();
          log.info("sending bulk request with " + currentRequest.numberOfActions() + " items");
          BulkResponse response = currentRequest.execute().actionGet();
          for(BulkItemResponse item : response) {
            checkItem(item);
          }
        } catch(Exception e) {
          log.warn("bulk request failure", e);
          throw new IOException(e);
        }
        this.currentRequest = this.client.prepareBulk();
      }
    }

    private void ensureStarted() {
      if(this.started == false) {
        this.started = true;
        prepareIndex();
      }
    }

    private void checkItem(BulkItemResponse item) {
      // TODO check the result
    }
  }

  @Override
  public RecordWriter<String, String> getRecordWriter(FileSystem ignored, JobConf job, String name,
      Progressable progress) throws IOException {
    return new ElasticSearchRecordWriter(job, progress);
  }

  @Override
  public void checkOutputSpecs(FileSystem ignored, JobConf job) throws IOException {

  }

}
