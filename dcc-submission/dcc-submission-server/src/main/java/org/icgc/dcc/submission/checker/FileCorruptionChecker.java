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

import java.util.List;
import java.util.regex.Pattern;

import org.icgc.dcc.submission.checker.Util.CheckLevel;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.ValidationErrorCode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * 
 */
public class FileCorruptionChecker extends CompositeFileChecker {

  public FileCorruptionChecker(FileChecker fileChecker, boolean isFailFast) {
    super(fileChecker, isFailFast);
  }

  public FileCorruptionChecker(FileChecker fileChecker) {
    this(fileChecker, true);
  }

  @Override
  public List<FirstPassValidationError> selfCheck(String filePathname) {
    Builder<FirstPassValidationError> errorBuilder = ImmutableList.<FirstPassValidationError> builder();
    Optional<FileSchema> fileSchema = getDictionary().fileSchema(getFileSchemaName(filePathname));
    if (fileSchema.isPresent()) {
      // more than 1 file that match the same pattern
      if (ImmutableList.copyOf(getSubmissionDirectory().listFile(Pattern.compile(fileSchema.get().getPattern())))
          .size() > 1) {
        errorBuilder.add(new FirstPassValidationError(CheckLevel.FILE_LEVEL,
            "More than 1 file matching the file pattern: " + fileSchema.get().getPattern(),
            ValidationErrorCode.TOO_MANY_FILES_ERROR));
      }
    }
    return errorBuilder.build();
  }

  @Override
  public boolean isFailFast() {
    return isFailFast;
  }
}
