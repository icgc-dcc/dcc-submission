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
package org.icgc.dcc.core.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * Util methods for files (compressed or not).
 */
public class Files2 {

  public static final String GZIP = "gzip";
  public static final String BZIP2 = "bzip2";

  public static final String GZIP_EXTENSION = Separators.EXTENSION + "gz";
  public static final String BZIP2_EXTENSION = Separators.EXTENSION + "bz2";

  private static final String GZIP_MEDIA_TYPE = "application/x-" + GZIP;
  private static final String BZIP2_MEDIA_TYPE = "application/x-" + BZIP2;

  /**
   * Compression formats supported: gzip and bzip2.
   */
  @SneakyThrows
  public static String getCompressionAgnosticFirstLine(@NonNull final String path) {
    @Cleanup
    val bufferedReader = getCompressionAgnosticBufferedReader(path);

    return bufferedReader.readLine();
  }

  /**
   * Compression formats supported: gzip and bzip2.
   */
  @SneakyThrows
  public static BufferedReader getCompressionAgnosticBufferedReader(@NonNull final String path) {
    val fileInputStream = new FileInputStream(path);
    return new BufferedReader(
        new InputStreamReader(

            // TODO: use cleaner mechanism (tika?)
            isGzip(path) ?
                new GZIPInputStream(fileInputStream) :
                isBzip2(path) ?
                    new BZip2CompressorInputStream(fileInputStream) :
                    fileInputStream));
  }

  @SneakyThrows
  public static InputStream getCompressionAgnosticInputStream(
      @NonNull final String path,
      @NonNull final String mediaType) {
    InputStream in = new FileInputStream(path);

    // Gzip
    if (isGzipMediaType(mediaType)) {
      in = new GZIPInputStream(in);
    }

    // Bzip2
    else if (isBzip2MediaType(mediaType)) {
      in = new BZip2CompressorInputStream(in);
    }

    return in;
  }

  private static boolean isGzip(@NonNull final String path) {
    return path.endsWith(GZIP_EXTENSION);
  }

  private static boolean isBzip2(@NonNull final String path) {
    return path.endsWith(BZIP2_EXTENSION);
  }

  private static boolean isGzipMediaType(@NonNull final String mediaType) {
    return GZIP_MEDIA_TYPE.equals(mediaType);
  }

  private static boolean isBzip2MediaType(@NonNull final String mediaType) {
    return BZIP2_MEDIA_TYPE.equals(mediaType);
  }

}
