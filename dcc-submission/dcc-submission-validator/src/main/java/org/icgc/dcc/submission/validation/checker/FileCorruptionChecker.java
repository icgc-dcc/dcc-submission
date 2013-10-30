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

import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.icgc.dcc.submission.validation.core.ValidationErrorCode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

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
  public List<FirstPassValidationError> performSelfCheck(String filename) {
    Builder<FirstPassValidationError> errors = ImmutableList.builder();
    try {
      switch (Util.determineCodec(getDccFileSystem(), getSubmissionDirectory(), filename)) {
      case GZIP:
        errors.addAll(checkGZip(filename));
        break;
      case BZIP2:
        errors.addAll(checkBZip2(filename));
        break;
      }
    } catch (IOException e) {
      log.info("Exception caught in reading file (corruption): {}", filename, e);
      errors.add(new FirstPassValidationError(getCheckLevel(), "Error in reading the file (corruption): "
          + filename,
          ValidationErrorCode.COMPRESSION_CODEC_ERROR, getFileSchemaName(filename)));
    }
    return errors.build();
  }

  private List<FirstPassValidationError> checkBZip2(String filename) {
    Builder<FirstPassValidationError> errors = ImmutableList.builder();
    try {
      // check the bzip2 header
      @Cleanup
      BZip2CompressorInputStream in =
          new BZip2CompressorInputStream(getDccFileSystem().open(getSubmissionDirectory().getDataFilePath(filename)));
      // see if it can be read through
      byte[] buf = new byte[BUFFER_SIZE];
      while (in.read(buf) > 0) {
      }
    } catch (IOException e) {
      log.info("Exception caught in decoding bzip2 file: {}", filename, e);
      errors.add(new FirstPassValidationError(getCheckLevel(), "Corrupted bzip file: " + filename,
          ValidationErrorCode.COMPRESSION_CODEC_ERROR, getFileSchemaName(filename)));
    }
    return errors.build();
  }

  private List<FirstPassValidationError> checkGZip(String filename) {
    Builder<FirstPassValidationError> errors = ImmutableList.builder();
    try {
      // check the gzip header
      @Cleanup
      GZIPInputStream in =
          new GZIPInputStream(getDccFileSystem().open(getSubmissionDirectory().getDataFilePath(filename)));
      // see if it can be read through
      byte[] buf = new byte[BUFFER_SIZE];
      while (in.read(buf) > 0) {
      }
    } catch (IOException e) {
      log.info("Exception caught in decoding gzip file: {}", filename, e);
      errors.add(new FirstPassValidationError(getCheckLevel(), "Corrupted gzip file: " + filename,
          ValidationErrorCode.COMPRESSION_CODEC_ERROR, getFileSchemaName(filename)));
    }
    return errors.build();
  }

}
