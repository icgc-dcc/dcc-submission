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

import static com.google.common.base.Preconditions.checkState;
import static lombok.AccessLevel.PACKAGE;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import cascading.scheme.Scheme;
import cascading.tap.SinkMode;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tuple.Fields;

/**
 * Utility class to help with hadoop {@link Tap}s from cascading.
 */
@NoArgsConstructor(access = PACKAGE)
public class HadoopTaps implements Taps {

  @Override
  public Tap<?, ?, ?> getNoCompressionTsvWithHeader(String path) {
    return Static.getNoCompressionTsvWithHeader(path);
  }

  @Override
  public Tap<?, ?, ?> getDecompressingTsvWithHeader(String path) {
    return Static.getDecompressingTsvWithHeader(path);
  }

  @Override
  public Tap<?, ?, ?> getDecompressingLinesNoHeader(String path, Fields numField, Fields lineField) {
    checkState(false, "Not implemented yet.");
    return null;
  }

  @Override
  public Tap<?, ?, ?> getDecompressingFileTap(Scheme<Properties, InputStream, OutputStream, ?, ?> scheme, String path) {
    checkState(false, "Not implemented yet.");
    return null;
  }

  private static class Static {

    public static Tap<?, ?, ?> getDecompressingTsvWithHeader(@NonNull final String path) {
      return new Hfs(
          GenericSchemes.getDecompressingHadoopTsvWithHeader(),
          path);
    }

    public static Tap<?, ?, ?> getNoCompressionTsvWithHeader(@NonNull final String path) {
      return new Hfs(
          GenericSchemes.getNoCompressionHadoopTsvWithHeader(),
          path,
          SinkMode.KEEP);
    }

  }

}
