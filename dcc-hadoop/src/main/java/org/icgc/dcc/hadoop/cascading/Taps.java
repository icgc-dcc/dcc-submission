/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.hadoop.cascading;

import static com.google.common.base.Preconditions.checkState;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.util.Files2.getCompressionAgnosticInputStream;
import static org.icgc.dcc.hadoop.cascading.Fields2.checkFieldsCardinalityOne;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.apache.tika.Tika;

import cascading.flow.FlowDef;
import cascading.flow.FlowProcess;
import cascading.scheme.Scheme;
import cascading.tap.SinkMode;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tuple.Fields;
import cascading.tuple.TupleEntryIterator;

import com.google.common.base.Function;

/**
 * Utility class to help with the {@link Tap} object from cascading.
 */
@NoArgsConstructor(access = PRIVATE)
public class Taps {

  public static final Tap<?, ?, ?> getNoCompressionLocalTsvWithHeader(
      @NonNull final String path) {

    return new cascading.tap.local.FileTap(
        Schemes.getLocalTsvWithHeader(),
        path);
  }

  public static final Tap<?, ?, ?> getDecompressingLocalTsvWithHeader(@NonNull final String path) {

    return getDecompressingLocalFileTap(
        Schemes.getDecompressingLocalTsvWithHeader(path),
        path);
  }

  public static final Tap<?, ?, ?> getDecompressingLocalLinesNoHeader(
      @NonNull final String path,
      @NonNull final Fields numField,
      @NonNull final Fields lineField) {

    return getDecompressingLocalFileTap(
        Schemes.getLocalLinesWithOffset(
            checkFieldsCardinalityOne(numField),
            checkFieldsCardinalityOne(lineField)),
        path);
  }

  public static Tap<?, ?, ?> getCompressingHadoopTsvWithHeader(@NonNull final String path) {
    return new Hfs(
        Schemes.getCompressingHadoopTsvWithHeader(),
        path);
  }

  public static Tap<?, ?, ?> getNoCompressionHadoopTsvFileWithHeader(@NonNull final String path) {
    return new Hfs(
        Schemes.getNoCompressionHadoopTsvWithHeader(),
        path,
        SinkMode.KEEP);
  }

  /**
   * Must suppress warning as cascading unfortunately uses raw types in {@link FlowDef#addSources(Map)}.
   */
  @SuppressWarnings("rawtypes")
  public static Function<Tap<?, ?, ?>, Tap> RAW_CASTER = new Function<Tap<?, ?, ?>, Tap>() {

    @Override
    public Tap apply(Tap<?, ?, ?> tap) {
      return tap;
    }

  };

  public static cascading.tap.local.FileTap getDecompressingLocalFileTap(
      @NonNull final Scheme<Properties, InputStream, OutputStream, ?, ?> scheme,
      @NonNull final String path) {

    return new cascading.tap.local.FileTap(scheme, path) {

      @Override
      public TupleEntryIterator openForRead(
          FlowProcess<Properties> flowProcess,
          InputStream input)
          throws IOException {
        checkState(input == null,
            "Expecting input to be null here, instead: '{}'",
            input == null ? null : input.getClass().getSimpleName());

        return super.openForRead(
            flowProcess,

            // Do not @Cleanup (cascading will close it)
            getCompressionAgnosticInputStream(
                path,
                new Tika().detect(getIdentifier())));
      }

    };
  }

}
