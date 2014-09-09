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

import static com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.util.Files2.getCompressionAgnosticFirstLine;
import static org.icgc.dcc.hadoop.cascading.Fields2.checkFieldsCardinalityOne;
import static org.icgc.dcc.hadoop.cascading.Fields2.fields;
import static org.icgc.dcc.hadoop.cascading.TupleEntries.getFirstObject;
import static org.icgc.dcc.hadoop.cascading.taps.GenericSchemes.TSV_DELIMITER;
import static org.icgc.dcc.hadoop.cascading.taps.GenericSchemes.noHeader;
import static org.icgc.dcc.hadoop.cascading.taps.GenericSchemes.withHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Properties;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import cascading.flow.FlowProcess;
import cascading.scheme.Scheme;
import cascading.scheme.SinkCall;
import cascading.scheme.SourceCall;
import cascading.scheme.local.TextDelimited;
import cascading.scheme.local.TextLine;
import cascading.tap.Tap;
import cascading.tuple.Fields;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Splitter;

/**
 * Utility class for working with cascading local {@code Scheme} objects, such as {@code TextDelimited}.
 * <p>
 * Do <b>not<b/> recycle {@link Scheme}s as they are actually mutated.
 */
@NoArgsConstructor(access = PRIVATE)
class LocalSchemes {

  static final TextLine getTextLine() {
    return new TextLine();
  }

  static final Scheme<Properties, InputStream, OutputStream, LineNumberReader, PrintWriter> getNoCompressionTsvNoHeader() {
    return new TextDelimited(
        noHeader(),
        TSV_DELIMITER);
  }

  static final Scheme<Properties, InputStream, OutputStream, LineNumberReader, PrintWriter> getNoCompressionTsvWithHeader() {
    return new TextDelimited(
        withHeader(),
        TSV_DELIMITER);
  }

  static final Scheme<Properties, InputStream, OutputStream, LineNumberReader, PrintWriter> getNoCompressionTsvWithHeader(
      @NonNull final Fields fields) {
    return new TextDelimited(
        fields,
        withHeader(),
        TSV_DELIMITER);
  }

  static final Scheme<Properties, InputStream, OutputStream, LineNumberReader, PrintWriter> getLinesWithOffset(
      @NonNull final Fields numField,
      @NonNull final Fields lineField) {
    return new TextLine(
        checkFieldsCardinalityOne(numField)
            .append(checkFieldsCardinalityOne(lineField)));
  }

  static final Scheme<Properties, InputStream, OutputStream, LineNumberReader, PrintWriter> getDecompressingTsvWithHeader(
      @NonNull final String path) {
    return new TextDelimited(
        withHeader(),
        TSV_DELIMITER) {

      private final Splitter splitter = Splitter.on(getDelimiter());

      @Override
      public Fields retrieveSourceFields(
          FlowProcess<Properties> process,
          @SuppressWarnings("rawtypes") Tap tap) { // as-is in cascading
        setSourceFields(fields(splitter.split(getCompressionAgnosticFirstLine(path))));
        return getSourceFields();
      }

    };
  }

  static final TextLine getJsonScheme() {
    return new TextLine() {

      private final transient ObjectWriter writer = new ObjectMapper(
          new JsonFactory().disable(AUTO_CLOSE_TARGET))
          .disable(FAIL_ON_EMPTY_BEANS).writerWithDefaultPrettyPrinter();

      @Override
      public void sourcePrepare(FlowProcess<Properties> flowProcess,
          SourceCall<LineNumberReader, InputStream> sourceCall)
          throws IOException {
        throw new IllegalStateException("JsonScheme cannot be used as a source.");
      }

      @Override
      public void sink(FlowProcess<Properties> flowProcess, SinkCall<PrintWriter, OutputStream> sinkCall)
          throws IOException {
        writer.writeValue(
            sinkCall.getContext(),
            getFirstObject(sinkCall.getOutgoingEntry()));
      }

    };
  }

}
