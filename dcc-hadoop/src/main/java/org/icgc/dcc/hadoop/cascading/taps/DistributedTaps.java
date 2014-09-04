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
package org.icgc.dcc.hadoop.cascading.taps;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PUBLIC;
import static org.icgc.dcc.hadoop.cascading.taps.GenericTaps.LINE_FIELD;
import static org.icgc.dcc.hadoop.cascading.taps.LegacySchemes.newHadoopLooseTsvScheme;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.icgc.dcc.hadoop.cascading.CascadingContext;

import cascading.scheme.hadoop.TextLine;
import cascading.scheme.hadoop.TextLine.Compress;
import cascading.tap.SinkMode;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tuple.Fields;

/**
 * Utility class to help with hadoop {@link Tap}s from cascading.
 * <p>
 * Do *not* use constructor outside fo {@link CascadingContext}.
 */
@NoArgsConstructor(access = PUBLIC)
public class DistributedTaps implements CascadingTaps {

  @Override
  public Tap<?, ?, ?> getLines(@NonNull final String path) {
    return Static.getLines(path);
  }

  @Override
  public Tap<?, ?, ?> getLines(
      @NonNull final String path,
      @NonNull final SinkMode sinkMode) {
    return Static.getLines(path, sinkMode);
  }

  @Override
  public Tap<?, ?, ?> getNoCompressionNoHeaderNonStrictTsv(String path) {
    return Static.getNoCompressionNoHeaderNonStrictTsv(path);
  }

  @Override
  public Tap<?, ?, ?> getNoCompressionTsvNoHeader(@NonNull final String path) {
    return Static.getNoCompressionTsvNoHeader(path);
  }

  @Override
  public Tap<?, ?, ?> getNoCompressionTsvWithHeader(@NonNull final String path) {
    return Static.getNoCompressionTsvWithHeader(path);
  }

  @Override
  public Tap<?, ?, ?> getNoCompressionTsvWithHeader(
      @NonNull final String path,
      @NonNull final Fields fields) {
    return Static.getNoCompressionTsvWithHeader(path, fields);
  }

  @Override
  public Tap<?, ?, ?> getDecompressingTsvWithHeader(String path) {
    return Static.getDecompressingTsvWithHeader(path);
  }

  @Override
  public Tap<?, ?, ?> getDecompressingLinesNoHeader(String path, Fields numField) {
    return Static.getDecompressingLinesNoHeader(path, numField);
  }

  @Override
  public Tap<?, ?, ?> getDecompressingLinesNoHeader(String path, Fields numField, Fields lineField) {
    return Static.getDecompressingLinesNoHeader(path, numField, lineField);
  }

  @Override
  public Tap<?, ?, ?> getCompressingJson(String path) {
    return Static.getJsonScheme(path);
  }

  @NoArgsConstructor(access = PRIVATE)
  private final static class Static {

    public static Tap<?, ?, ?> getLines(@NonNull final String path) {
      return getLines(path, SinkMode.KEEP);
    }

    public static Tap<?, ?, ?> getLines(
        @NonNull final String path,
        @NonNull final SinkMode sinkMode) {
      return new Hfs(
          DistributedSchemes.getTextLine(),
          path,
          sinkMode);
    }

    public static Tap<?, ?, ?> getNoCompressionNoHeaderNonStrictTsv(@NonNull final String path) {
      return new Hfs(
          newHadoopLooseTsvScheme(), // TODO: move to DistributedSchemes (+rewrite)
          path);
    }

    public static Tap<?, ?, ?> getDecompressingTsvWithHeader(@NonNull final String path) {
      return new Hfs(
          DistributedSchemes.getDecompressingTsvWithHeader(),
          path);
    }

    public static Tap<?, ?, ?> getNoCompressionTsvWithHeader(@NonNull final String path) {
      return new Hfs(
          DistributedSchemes.getNoCompressionTsvWithHeader(),
          path);
    }

    public static Tap<?, ?, ?> getNoCompressionTsvNoHeader(
        @NonNull final String path) {
      return new Hfs(
          DistributedSchemes.getNoCompressionTsvNoHeader(),
          path);
    }

    public static Tap<?, ?, ?> getNoCompressionTsvWithHeader(
        @NonNull final String path,
        @NonNull final Fields declaredFields) {
      return new Hfs(
          DistributedSchemes.getNoCompressionTsvWithHeader(declaredFields),
          path);
    }

    public static Tap<?, ?, ?> getDecompressingLinesNoHeader(
        @NonNull final String path,
        @NonNull final Fields numField) {
      return getDecompressingLinesNoHeader(path, numField, LINE_FIELD);
    }

    public static Tap<?, ?, ?> getDecompressingLinesNoHeader(
        @NonNull final String path,
        @NonNull final Fields numField,
        @NonNull final Fields lineField) {
      return new Hfs(
          enableSinkCompression(
          new TextLine(
              numField
                  .append(lineField))),
          path);
    }

    public static Tap<?, ?, ?> getJsonScheme(@NonNull final String path) {
      return new Hfs(
          enableSinkCompression(DistributedSchemes.getJsonScheme()),
          path);
    }

    private static TextLine enableSinkCompression(@NonNull final TextLine scheme) {
      scheme.setSinkCompression(Compress.ENABLE);
      return scheme;
    }

  }

}
