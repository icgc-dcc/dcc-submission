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
package org.icgc.dcc.submission.validation.first.step;

import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.COMPRESSION_CODEC_ERROR;

import java.io.IOException;
import java.util.zip.GZIPInputStream;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.icgc.dcc.submission.validation.first.Util;
import org.icgc.dcc.submission.validation.first.Util.CodecType;
import org.icgc.dcc.submission.validation.first.FileChecker;

@Slf4j
public class FileCorruptionChecker extends CompositeFileChecker {

  private static final int BUFFER_SIZE = 65536;

  public FileCorruptionChecker(FileChecker fileChecker, boolean failFast) {
    super(fileChecker, failFast);
  }

  public FileCorruptionChecker(FileChecker fileChecker) {
    this(fileChecker, true);
  }

  @Override
  public void performSelfCheck(String filename) {
    try {
      CodecType contentType = Util.determineCodecFromContent(getFs(), filename);
      CodecType filenameType = Util.determineCodecFromFilename(filename);
      if (contentType == filenameType) {
        switch (contentType) {
        case GZIP:
          checkGZip(filename);
          break;
        case BZIP2:
          checkBZip2(filename);
          break;
        case PLAIN_TEXT:
          // Do nothing
          break;
        }
      } else {
        log.info("Content type does not match the extension for file: " + filename);
        // TODO: create new error type rather?

        incrementCheckErrorCount();

        getReportContext().reportError(
            error()
                .fileName(filename)
                .type(COMPRESSION_CODEC_ERROR)
                .params(getFileSchema(filename).getName())
                .build());
      }
    } catch (IOException e) {
      log.info("Exception caught in reading file (corruption): {}", filename, e);

      incrementCheckErrorCount();

      getReportContext().reportError(
          error()
              .fileName(filename)
              .type(COMPRESSION_CODEC_ERROR)
              .params(getFileSchema(filename).getName())
              .build());
    }
  }

  /**
   * TODO: merge with gzip one with a flag for the input stream based on the type.
   */
  private void checkBZip2(String filename) {
    try {
      // check the bzip2 header
      @Cleanup
      BZip2CompressorInputStream in = new BZip2CompressorInputStream(getFs().getDataInputStream(filename));

      // see if it can be read through
      byte[] buf = new byte[BUFFER_SIZE];
      while (in.read(buf) > 0) {
      }
    } catch (IOException e) {
      log.info("Exception caught in decoding bzip2 file '{}': '{}'", filename, e.getMessage());

      incrementCheckErrorCount();

      getReportContext().reportError(
          error()
              .fileName(filename)
              .type(COMPRESSION_CODEC_ERROR)
              .params(getFileSchema(filename).getName())
              .build());
    }
  }

  private void checkGZip(String filename) {
    try {
      // check the gzip header
      @Cleanup
      GZIPInputStream in = new GZIPInputStream(getFs().getDataInputStream(filename));

      // see if it can be read through
      byte[] buf = new byte[BUFFER_SIZE];
      while (in.read(buf) > 0) {
      }
    } catch (IOException e) {
      log.info("Exception caught in decoding gzip file '{}': '{}'", filename, e.getMessage());

      incrementCheckErrorCount();

      getReportContext().reportError(
          error()
              .fileName(filename)
              .type(COMPRESSION_CODEC_ERROR)
              .params(getFileSchema(filename).getName())
              .build());
    }
  }
}
