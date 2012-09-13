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
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import cascading.flow.FlowProcess;
import cascading.tap.SinkTap;
import cascading.tap.Tap;
import cascading.tap.hadoop.io.HadoopTupleEntrySchemeCollector;
import cascading.tuple.TupleEntryCollector;

public class ElasticSearchTap extends SinkTap<JobConf, OutputCollector<String, String>> {

  private final URI esUri;

  private final String index;

  private final String type;

  // TODO: fix the URI so we extract the index and type from it
  public ElasticSearchTap(ElasticSearchScheme scheme, URI uri, String index, String type) {
    super(scheme);
    this.esUri = uri;
    this.index = index;
    this.type = type;
  }

  public ElasticSearchTap(ElasticSearchScheme scheme, String uri, String index, String type) {
    this(scheme, parseURI(uri), index, type);
  }

  @Override
  public String getIdentifier() {
    return esUri.toString();
  }

  @Override
  public void sinkConfInit(FlowProcess<JobConf> flowProcess, JobConf conf) {
    super.sinkConfInit(flowProcess, conf);

    FileOutputFormat.setOutputPath(conf, new Path("/tmp/potatoe/icgc/"));

    conf.setOutputKeyClass(String.class);
    conf.setOutputValueClass(String.class);
    conf.setOutputFormat(ElasticSearchOutputFormat.class);
    ElasticSearchConfig es = new ElasticSearchConfig(conf);
    es.setIndex(index);
    es.setType(type);
    es.addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
  }

  @Override
  public TupleEntryCollector openForWrite(FlowProcess<JobConf> flowProcess, OutputCollector<String, String> output)
      throws IOException {
    return new HadoopTupleEntrySchemeCollector(flowProcess, (Tap) this, output);
  }

  @Override
  public boolean createResource(JobConf conf) throws IOException {
    return true;
  }

  @Override
  public boolean deleteResource(JobConf conf) throws IOException {
    return true;
  }

  @Override
  public boolean resourceExists(JobConf conf) throws IOException {
    return false;
  }

  @Override
  public long getModifiedTime(JobConf conf) throws IOException {
    return System.currentTimeMillis();
  }

  private static final URI parseURI(String uri) {
    try {
      return new URI(uri);
    } catch(URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

}
