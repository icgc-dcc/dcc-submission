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
package org.icgc.dcc.submission.validation.checker;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.Cleanup;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.tika.Tika;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;

final class Util {

  public enum CheckLevel {
    FILE_LEVEL, ROW_LEVEL, CELL_LEVEL;
  }

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

  public static CodecType determineCodecFromContent(DccFileSystem fs, SubmissionDirectory submissionDirectory,
      String filename)
      throws IOException {
    @Cleanup
    BufferedInputStream bis =
        new BufferedInputStream(fs.open(submissionDirectory.getDataFilePath(filename)));
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

  public static InputStream createInputStream(DccFileSystem dccFileSystem, String filePathname) throws IOException {
    Configuration conf = dccFileSystem.getFileSystem().getConf();
    CompressionCodecFactory factory = new CompressionCodecFactory(conf);
    Path filePath = new Path(filePathname);
    CompressionCodec codec = factory.getCodec(filePath);
    if (codec == null) {
      // This is assumed to be PLAIN_TEXT
      return dccFileSystem.open(filePathname);
    } else {
      return codec.createInputStream(dccFileSystem.open(filePathname));
    }
  }
}
