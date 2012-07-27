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

package org.icgc.dcc.validation.cascading;

import java.io.IOException;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator.Feature;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.SerializationConfig;

import cascading.flow.FlowProcess;
import cascading.scheme.SinkCall;
import cascading.scheme.hadoop.TextLine;

public class HadoopJsonScheme extends TextLine {

  private transient ObjectMapper mapper = new ObjectMapper(new JsonFactory().disable(Feature.AUTO_CLOSE_TARGET))
      .disable(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS);

  private transient ObjectWriter writer = mapper().writerWithDefaultPrettyPrinter();

  public HadoopJsonScheme() {

  }

  @Override
  public void sink(FlowProcess<JobConf> flowProcess, SinkCall<Object[], OutputCollector> sinkCall) throws IOException {
    Object report = sinkCall.getOutgoingEntry().getTuple().getObject(0);
    // it's ok to use NULL here so the collector does not write anything
    sinkCall.getOutput().collect(null, writer().writeValueAsString(report));

  }

  private final ObjectMapper mapper() {
    if(mapper == null) {
      mapper =
          new ObjectMapper(new JsonFactory().disable(Feature.AUTO_CLOSE_TARGET))
              .disable(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS);
    }

    return mapper;
  }

  private final ObjectWriter writer() {
    if(writer == null) {
      writer = mapper().writerWithDefaultPrettyPrinter();
    }

    return writer;
  }

}
