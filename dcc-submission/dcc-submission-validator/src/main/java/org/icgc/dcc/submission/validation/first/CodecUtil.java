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
package org.icgc.dcc.submission.validation.first;

import static lombok.AccessLevel.PRIVATE;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.Cleanup;
import lombok.NoArgsConstructor;
import lombok.val;

import org.apache.tika.Tika;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.icgc.dcc.submission.fs.SubmissionDirectory;

/**
 * TODO: move this to {@link FPVFileSystem} (some to {@link SubmissionDirectory} even).
 */
@NoArgsConstructor(access = PRIVATE)
public final class CodecUtil {

  public enum CodecType {
    GZIP, BZIP2, PLAIN_TEXT;
  }

  public static CodecType determineCodecFromFilename(String filename)
  {
    Tika tika = new Tika();
    String mediaType = tika.detect(filename);
    if (mediaType.equals("application/x-gzip")) {
      return CodecType.GZIP;
    } else if (mediaType.equals("application/x-bzip2")) {
      return CodecType.BZIP2;
    }

    return CodecType.PLAIN_TEXT;
  }

  public static CodecType determineCodecFromContent(FPVFileSystem fs, String filename) throws IOException {
    @Cleanup
    BufferedInputStream bis = new BufferedInputStream(fs.getDataInputStream(filename));
    AutoDetectParser parser = new AutoDetectParser();
    Detector detector = parser.getDetector();
    Metadata md = new Metadata();
    md.add(Metadata.RESOURCE_NAME_KEY, filename);

    String mediaType = detector.detect(bis, md).toString();
    if (mediaType.equals("application/x-gzip")) {
      return CodecType.GZIP;
    } else if (mediaType.equals("application/x-bzip2")) {
      return CodecType.BZIP2;
    }

    return CodecType.PLAIN_TEXT;
  }

  public static InputStream createInputStream(FPVFileSystem fs, String fileName) throws IOException {
    val codec = fs.getCompressionCodec(fileName);
    val in = fs.getDataInputStream(fileName);
    return codec == null ?
        in : // This is assumed to be PLAIN_TEXT
        codec.createInputStream(in);
  }

}
