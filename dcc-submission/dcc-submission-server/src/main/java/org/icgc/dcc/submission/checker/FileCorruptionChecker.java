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
package org.icgc.dcc.submission.checker;

import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;

import lombok.Cleanup;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.validation.ValidationErrorCode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class FileCorruptionChecker extends CompositeFileChecker {

  private static final int BUFFER_SIZE = 65536;
  final private DccFileSystem fs;

  public FileCorruptionChecker(FileChecker fileChecker, DccFileSystem fs, boolean failFast) {
    super(fileChecker, failFast);
    this.fs = fs;
  }

  public FileCorruptionChecker(FileChecker fileChecker, DccFileSystem fs) {
    this(fileChecker, fs, true);
  }

  @Override
  public List<FirstPassValidationError> performSelfCheck(String filename) {
    Builder<FirstPassValidationError> errors = ImmutableList.builder();
    try {
      switch (Util.determineCodec(fs, getSubmissionDirectory(), filename)) {
      case GZIP:
        checkGzip(filename);
        break;
      case BZIP2:
        checkBzip(filename);
        break;
      }
    } catch (IOException e) {
      errors.add(new FirstPassValidationError(getCheckLevel(), "Error in reading the file (corruption): "
          + filename,
          ValidationErrorCode.COMPRESSION_CODEC_ERROR));
    }
    return errors.build();
  }

  private List<FirstPassValidationError> checkBzip(String filename) {
    Builder<FirstPassValidationError> errors = ImmutableList.builder();
    try {
      // check the bzip2 header
      @Cleanup
      BZip2CompressorInputStream in =
          new BZip2CompressorInputStream(fs.open(getSubmissionDirectory().getDataFilePath(filename)));
      // see if it can be read through
      byte[] buf = new byte[BUFFER_SIZE];
      while (in.read(buf) > 0) {
      }
    } catch (IOException e) {
      errors.add(new FirstPassValidationError(getCheckLevel(), "Corrupted bzip file: " + filename,
          ValidationErrorCode.COMPRESSION_CODEC_ERROR));
    }
    return errors.build();
  }

  private List<FirstPassValidationError> checkGzip(String filename) {
    Builder<FirstPassValidationError> errors = ImmutableList.builder();
    try {
      // check the gzip header
      @Cleanup
      GZIPInputStream in = new GZIPInputStream(fs.open(getSubmissionDirectory().getDataFilePath(filename)));
      // see if it can be read through
      byte[] buf = new byte[BUFFER_SIZE];
      while (in.read(buf) > 0) {
      }
    } catch (IOException e) {
      errors.add(new FirstPassValidationError(getCheckLevel(), "Corrupted gzip file: " + filename,
          ValidationErrorCode.COMPRESSION_CODEC_ERROR));
    }
    return errors.build();
  }

}
