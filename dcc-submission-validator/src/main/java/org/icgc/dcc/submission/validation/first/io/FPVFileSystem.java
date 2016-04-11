/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.first.io;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.regex.Pattern.compile;
import static org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy.FIELD_SPLITTER;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.compress.BZip2Codec;
import org.apache.hadoop.io.compress.CompressionInputStream;
import org.apache.tika.Tika;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.icgc.dcc.submission.fs.SubmissionDirectory;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * Class representing interactions with the file system in the context of FPV (as a temporary measure to isolate such
 * operations from the FPV at first).
 * <p>
 * TODO: add test for this class (especially after merging {@link Util} in it)
 */
@RequiredArgsConstructor
public class FPVFileSystem {

  public enum CodecType {
    GZIP, BZIP2, PLAIN_TEXT;
  }

  private static final int BUFFER_SIZE = 65536;

  private final SubmissionDirectory submissionDirectory;

  public InputStream getDecompressingInputStream(String fileName) {
    return submissionDirectory.getDecompressingInputStream(fileName);
  }

  public Iterable<String> listMatchingSubmissionFiles(Iterable<String> filePatterns) {
    return submissionDirectory.listFiles(filePatterns);
  }

  public List<String> getMatchingFileNames(String pattern) {
    return copyOf(submissionDirectory.listFile(compile(pattern)));
  }

  public CodecType determineCodecFromFilename(String fileName) {
    Tika tika = new Tika();
    String mediaType = tika.detect(fileName);
    if (mediaType.equals("application/x-gzip")) {
      return CodecType.GZIP;
    } else if (mediaType.equals("application/x-bzip2")) {
      return CodecType.BZIP2;
    }

    return CodecType.PLAIN_TEXT;
  }

  public CodecType determineCodecFromContent(String fileName) throws IOException {
    @Cleanup
    BufferedInputStream bis = new BufferedInputStream(submissionDirectory.open(fileName));
    AutoDetectParser parser = new AutoDetectParser();
    Detector detector = parser.getDetector();
    Metadata md = new Metadata();
    md.add(Metadata.RESOURCE_NAME_KEY, fileName);

    String mediaType = detector.detect(bis, md).toString(); // FIXME: shouldn't rely on toString()...
    if (mediaType.equals("application/x-gzip")) {
      return CodecType.GZIP;
    } else if (mediaType.equals("application/x-bzip2")) {
      return CodecType.BZIP2;
    }

    return CodecType.PLAIN_TEXT;
  }

  public void attemptGzipRead(String fileName) throws IOException {
    // check the gzip header
    @Cleanup
    GZIPInputStream in = new GZIPInputStream(submissionDirectory.open(fileName));

    // see if it can be read through
    byte[] buf = new byte[BUFFER_SIZE];
    while (in.read(buf) > 0) {
    }
  }

  public void attemptBzip2Read(String fileName) throws IOException {
    // Check the bzip2 header
    BZip2Codec codec = new BZip2Codec();

    // FIXME: Passing in a blank configuration to get things working in CDH5.1 for now
    codec.setConf(new Configuration());

    @Cleanup
    CompressionInputStream in = codec.createInputStream(submissionDirectory.open(fileName));

    // see if it can be read through
    byte[] buf = new byte[BUFFER_SIZE];
    while (in.read(buf) > 0) {
    }
  }

  /**
   * Files are expected to be present and uncorrupted at this stage.
   */
  @SneakyThrows
  public List<String> peekFileHeader(String fileName) {
    @Cleanup
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(submissionDirectory.getDecompressingInputStream(fileName)));
    String header = reader.readLine();
    header = (header == null) ? "" : header;
    return copyOf(FIELD_SPLITTER.split(header));
  }

}
