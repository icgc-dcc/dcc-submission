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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import lombok.Cleanup;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ErrorCode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class FileHeaderChecker extends CompositeFileChecker {

  public FileHeaderChecker(FileChecker fileChecker, boolean failFast) {
    super(fileChecker, failFast);
  }

  public FileHeaderChecker(FileChecker fileChecker) {
    this(fileChecker, true);
  }

  @Override
  public List<FirstPassValidationError> performSelfCheck(String filename) {
    Builder<FirstPassValidationError> errors = ImmutableList.builder();

    try {

      List<String> expectedHeader = retrieveExpectedHeader(filename);
      List<String> actualHeader = peekFileHeader(filename);
      if (!actualHeader.equals(expectedHeader)) {
        errors.add(new FirstPassValidationError(getCheckLevel(), "Different from the expected header: "
            + expectedHeader
            + ", actual header: " + actualHeader, ErrorCode.FILE_HEADER_ERROR, actualHeader));
      }
    } catch (IOException e) {
      errors.add(new FirstPassValidationError(getCheckLevel(), "Unable to peek the file header for file: "
          + filename, ErrorCode.FILE_HEADER_ERROR, ImmutableList.<String> of()));
    }
    // check if they contain the same elements in the same order
    return errors.build();
  }

  private final List<String> retrieveExpectedHeader(String filename) {
    Optional<FileSchema> fileSchema = getDictionary().fileSchema(getFileSchemaName(filename));
    if (fileSchema.isPresent()) {
      return ImmutableList.copyOf(fileSchema.get().getFieldNames());
    }
    return ImmutableList.of();
  }

  private final List<String> peekFileHeader(String filename) throws IOException {
    InputStream is = Util.createInputStream(getDccFileSystem(), getSubmissionDirectory().getDataFilePath(filename));
    @Cleanup
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String header = reader.readLine();
    return ImmutableList.copyOf(header.split("\\t"));
  }
}
