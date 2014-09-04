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
package org.icgc.dcc.hadoop.cascading.taps;

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.hadoop.cascading.TupleEntries.getFirstObject;
import static org.icgc.dcc.hadoop.cascading.taps.GenericSchemes.TSV_DELIMITER;
import static org.icgc.dcc.hadoop.cascading.taps.GenericSchemes.noHeader;
import static org.icgc.dcc.hadoop.cascading.taps.GenericSchemes.withHeader;

import java.io.IOException;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.RecordReader;

import cascading.flow.FlowProcess;
import cascading.scheme.Scheme;
import cascading.scheme.SinkCall;
import cascading.scheme.SourceCall;
import cascading.scheme.hadoop.TextDelimited;
import cascading.scheme.hadoop.TextLine;
import cascading.scheme.hadoop.TextLine.Compress;
import cascading.tuple.Fields;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Utility class for working with cascading hadoop {@code Scheme} objects, such as {@code TextDelimited}.
 * <p>
 * Do <b>not<b/> recycle {@link Schemes2} as they are actually mutated.
 */
@NoArgsConstructor(access = PRIVATE)
class DistributedSchemes {

  static final TextLine getTextLine() {
    return new TextLine();
  }

  @SuppressWarnings("rawtypes")
  static final Scheme<JobConf, RecordReader, OutputCollector, Object[], Object[]> getDecompressingTsvWithHeader() {
    val textLine = getNoCompressionTsvWithHeader();
    textLine.setSinkCompression(Compress.ENABLE);
    return textLine;
  }

  static final TextLine getNoCompressionTsvWithHeader() {
    return new TextDelimited(
        withHeader(),
        TSV_DELIMITER);
  }

  static final TextLine getNoCompressionTsvNoHeader() {
    return new TextDelimited(
        noHeader(),
        TSV_DELIMITER);
  }

  static final TextLine getNoCompressionTsvWithHeader(@NonNull final Fields fields) {
    return new TextDelimited(
        fields,
        withHeader(),
        TSV_DELIMITER);
  }

  static final TextLine getJsonScheme() {
    return new TextLine() {

      private transient ObjectMapper mapper;

      private transient ObjectWriter writer;

      @Override
      @SuppressWarnings("rawtypes")
      public void sourcePrepare(FlowProcess<JobConf> flowProcess, SourceCall<Object[], RecordReader> sourceCall) {
        throw new IllegalStateException("JsonScheme cannot be used as a source.");
      }

      /**
       * It's ok to use NULL here so the collector does not write anything
       */
      @Override
      @SuppressWarnings({ "unchecked", "rawtypes" })
      public void sink(FlowProcess<JobConf> flowProcess, SinkCall<Object[], OutputCollector> sinkCall)
          throws IOException {
        sinkCall.getOutput().collect(
            null,
            writer()
                .writeValueAsString(
                    getFirstObject(sinkCall.getOutgoingEntry())));
      }

      private final ObjectMapper mapper() {
        if (mapper == null) {
          mapper =
              new ObjectMapper(new JsonFactory()
                  .disable(Feature.AUTO_CLOSE_TARGET))
                  .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        }

        return mapper;
      }

      private final ObjectWriter writer() {
        if (writer == null) {
          writer = mapper().writer();
        }

        return writer;
      }

    };
  }

}