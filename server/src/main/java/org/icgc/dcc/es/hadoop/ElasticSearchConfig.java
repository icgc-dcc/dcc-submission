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
import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.elasticsearch.common.transport.TransportAddress;

import cascading.flow.hadoop.util.HadoopUtil;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * 
 */
public class ElasticSearchConfig {

  public static final String INDEX_NAME = "es.index.name";

  public static final String TYPE_NAME = "es.type.name";

  public static final String BULK_SIZE = "es.bulk.size";

  private final Configuration conf;

  public ElasticSearchConfig(Configuration conf) {
    this.conf = conf;
  }

  public void setIndex(String index) {
    conf.set(INDEX_NAME, index);
  }

  public String getIndex() {
    return conf.get(INDEX_NAME);
  }

  public void setType(String type) {
    conf.set(TYPE_NAME, type);
  }

  public String getType() {
    return conf.get(TYPE_NAME);
  }

  public void setBulkSize(int bulkSize) {
    conf.setInt(BULK_SIZE, bulkSize);
  }

  public int getBulkSize() {
    return conf.getInt(BULK_SIZE, 100);
  }

  public void addTransportAddress(TransportAddress address) {
    try {
      String base64Address = HadoopUtil.serializeBase64(address, new JobConf(conf));
      String[] addresses = conf.getStrings("es.address");
      if(addresses == null) {
        conf.setStrings("es.address", base64Address);
      } else {
        // TODO
      }
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Iterable<TransportAddress> getTransportAddresses() {
    return Iterables.transform(Arrays.asList(conf.getStrings("es.address")), new Function<String, TransportAddress>() {

      @Override
      public TransportAddress apply(String input) {
        try {
          return HadoopUtil.deserializeBase64(input, new JobConf(conf), TransportAddress.class);
        } catch(IOException e) {
          throw new RuntimeException(e);

        }
      }
    });
  }
}
