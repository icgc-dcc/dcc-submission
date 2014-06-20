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
import static org.icgc.dcc.hadoop.cascading.Fields2.checkFieldsCardinalityOne;
import static org.icgc.dcc.hadoop.cascading.Fields2.fields;
import static org.icgc.dcc.hadoop.cascading.taps.GenericSchemes.TSV_DELIMITER;
import static org.icgc.dcc.hadoop.cascading.taps.GenericSchemes.withHeader;

import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Properties;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import cascading.flow.FlowProcess;
import cascading.scheme.Scheme;
import cascading.scheme.local.TextDelimited;
import cascading.scheme.local.TextLine;
import cascading.tap.Tap;
import cascading.tuple.Fields;

import com.google.common.base.Splitter;

/**
 * Utility class for working with cascading local {@code Scheme} objects, such as {@code TextDelimited}.
 * <p>
 * Do <b>not<b/> recycle {@link Scheme}s as they are actually mutated.
 */
@NoArgsConstructor(access = PRIVATE)
class LocalSchemes {

  static Scheme<Properties, InputStream, OutputStream, LineNumberReader, PrintWriter> getTsvWithHeader() {
    return new TextDelimited(withHeader(), TSV_DELIMITER);
  }

  static Scheme<Properties, InputStream, OutputStream, LineNumberReader, PrintWriter> getLinesWithOffset(
      @NonNull final Fields numField,
      @NonNull final Fields lineField) {

    return new TextLine(
        checkFieldsCardinalityOne(numField)
            .append(checkFieldsCardinalityOne(lineField)));
  }

  static Scheme<Properties, InputStream, OutputStream, LineNumberReader, PrintWriter> getDecompressingTsvWithHeader(
      @NonNull final String path) {
    return new TextDelimited(
        withHeader(),
        TSV_DELIMITER) {

      private final Splitter TSV_SPLITTER = Splitter.on(TSV_DELIMITER);

      @Override
      public Fields retrieveSourceFields(
          FlowProcess<Properties> process,
          @SuppressWarnings("rawtypes") Tap tap) { // as-is in cascading
        setSourceFields(fields(TSV_SPLITTER.split(getCompressionAgnosticFirstLine(path))));
        return getSourceFields();
      }

    };
  }

}
