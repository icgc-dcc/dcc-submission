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
package org.icgc.dcc.submission.validation.first.file;

import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.COMPRESSION_CODEC_ERROR;
import static org.icgc.dcc.submission.core.report.ErrorType.UNSUPPORTED_COMPRESSED_FILE;

import java.io.IOException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.first.core.FileChecker;
import org.icgc.dcc.submission.validation.first.io.FPVFileSystem.CodecType;

@Slf4j
public class FileCorruptionChecker extends DelegatingFileChecker {

  public FileCorruptionChecker(FileChecker fileChecker, boolean failFast) {
    super(fileChecker, failFast);
  }

  public FileCorruptionChecker(FileChecker fileChecker) {
    this(fileChecker, true);
  }

  @Override
  public void performSelfCheck(String fileName) {
    val fs = getFileSystem();

    CodecType fileNameType = fs.determineCodecFromFilename(fileName);
    log.info("File name '{}' indicates type: '{}'", fileName, fileNameType);

    CodecType contentType = null;
    try {
      contentType = fs.determineCodecFromContent(fileName);
    } catch (IOException e) {
      log.info("Exception caught in detecting file type for '{}' from content'{}'", fileName, e.getMessage());

      reportError(error()
          .fileName(fileName)
          .type(COMPRESSION_CODEC_ERROR) // TODO: create new "corrupted" file error rather
          .params(getFileSchema(fileName).getName())
          .build());
    }
    log.info("Content for '{}' indicates type: '{}'", fileName, contentType);

    if (contentType == fileNameType) {
      log.info("Check '{}' integrity of '{}'", contentType, fileName);
      switch (contentType) {
      case GZIP:
        checkGZip(fileName);
        break;
      case BZIP2:
        checkBZip2(fileName);
        break;
      case PLAIN_TEXT:
        // Do nothing
        break;
      }
    } else {
      log.info("Content type does not match the extension for file: '{}' ('{}' != '{}')",
          new Object[] { fileName, contentType, fileNameType });
      // TODO: create new error type rather?

      reportError(error()
          .fileName(fileName)
          .type(COMPRESSION_CODEC_ERROR)
          .params(getFileSchema(fileName).getName())
          .build());
    }
  }

  /**
   * TODO: merge with gzip one with a flag for the input stream based on the type.
   */
  private void checkBZip2(String fileName) {
    try {
      getFileSystem().attemptBzip2Read(fileName);
    } catch (IOException e) {
      e.printStackTrace();
      String errMsg = e.getMessage();
      log.info("Exception caught in decoding bzip2 file '{}': '{}'", fileName, errMsg);

      // TODO: remove this after upgrade hadoop
      if (errMsg != null && errMsg.equals("bad block header")) {
        log.info("found possibly, concatenated bzip2 files!", fileName);
        reportError(error()
            .fileName(fileName)
            .type(UNSUPPORTED_COMPRESSED_FILE)
            .params(getFileSchema(fileName).getName())
            .build());
      } else {
        reportError(error()
            .fileName(fileName)
            .type(COMPRESSION_CODEC_ERROR)
            .params(getFileSchema(fileName).getName())
            .build());
      }
    }
  }

  private void checkGZip(String fileName) {
    try {
      getFileSystem().attemptGzipRead(fileName);
    } catch (IOException e) {
      log.info("Exception caught in decoding gzip file '{}': '{}'", fileName, e.getMessage());

      reportError(error()
          .fileName(fileName)
          .type(COMPRESSION_CODEC_ERROR)
          .params(getFileSchema(fileName).getName())
          .build());
    }
  }
}
