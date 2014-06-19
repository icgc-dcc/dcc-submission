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
import static org.icgc.dcc.core.util.Files2.getCompressionAgnosticFirstLine;
import static org.icgc.dcc.core.util.Splitters.TAB;
import static org.icgc.dcc.hadoop.cascading.Fields2.checkFieldsCardinalityOne;
import static org.icgc.dcc.hadoop.cascading.Fields2.fields;

import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Properties;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.RecordReader;
import org.icgc.dcc.core.util.Separators;

import cascading.flow.FlowProcess;
import cascading.scheme.Scheme;
import cascading.scheme.hadoop.TextLine.Compress;
import cascading.tap.Tap;
import cascading.tuple.Fields;

/**
 * Utility class for working with cascading {@code Schemes} (local/hadoop agnostic) objects.
 * <p>
 * Do <b>not<b/> recycle {@link Schemes2} as they are actually mutated.
 * <p>
 * TODO: homogenize names and generalize return types when possible + do not expose other than to {@link LocalTaps}.
 */
@NoArgsConstructor(access = PRIVATE)
public class GenericSchemes {

  static final String TSV_DELIMITER = Separators.TAB;

  static Scheme<Properties, InputStream, OutputStream, LineNumberReader, PrintWriter> getLocalTsvWithHeader() {
    return new cascading.scheme.local.TextDelimited(withHeader(), Separators.TAB);
  }

  static Scheme<Properties, InputStream, OutputStream, LineNumberReader, PrintWriter> getLocalLinesWithOffset(
      @NonNull final Fields numField,
      @NonNull final Fields lineField) {

    return new cascading.scheme.local.TextLine(
        checkFieldsCardinalityOne(numField)
            .append(checkFieldsCardinalityOne(lineField)));
  }

  static Scheme<Properties, InputStream, OutputStream, LineNumberReader, PrintWriter> getDecompressingLocalTsvWithHeader(
      @NonNull final String path) {
    return new cascading.scheme.local.TextDelimited(
        withHeader(),
        TSV_DELIMITER) {

      @Override
      public Fields retrieveSourceFields(
          FlowProcess<Properties> process,
          @SuppressWarnings("rawtypes") Tap tap) { // as-is in cascading
        setSourceFields(fields(TAB.split(getCompressionAgnosticFirstLine(path))));
        return getSourceFields();
      }

    };
  }

  @SuppressWarnings("rawtypes")
  static Scheme<JobConf, RecordReader, OutputCollector, Object[], Object[]> getDecompressingHadoopTsvWithHeader() {
    val textLine = getNoCompressionHadoopTsvWithHeader();
    textLine.setSinkCompression(Compress.ENABLE);
    return textLine;
  }

  static cascading.scheme.hadoop.TextLine getNoCompressionHadoopTsvWithHeader() {
    return new cascading.scheme.hadoop.TextDelimited(
        withHeader(),
        TSV_DELIMITER);
  }

  static boolean withHeader() {
    return true;
  }

}
