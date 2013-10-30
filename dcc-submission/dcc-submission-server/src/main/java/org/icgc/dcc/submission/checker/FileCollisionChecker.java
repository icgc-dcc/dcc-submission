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
public class FileCollisionChecker extends CompositeFileChecker {

  public FileCollisionChecker(FileChecker fileChecker, boolean failFast) {
    super(fileChecker, failFast);
  }

  public FileCollisionChecker(FileChecker fileChecker) {
    this(fileChecker, true);
  }

  @Override
  public List<FirstPassValidationError> performSelfCheck(String filename) {
    Builder<FirstPassValidationError> errors = ImmutableList.<FirstPassValidationError> builder();
    Optional<FileSchema> fileSchema = getDictionary().fileSchema(getFileSchemaName(filename));
    if (fileSchema.isPresent()) {
      // more than 1 file that match the same pattern
      List<String> files =
          ImmutableList.copyOf(getSubmissionDirectory().listFile(Pattern.compile(fileSchema.get().getPattern())));
      if (files.size() > 1) {
        Object[] params = new Object[2];
        params[0] = fileSchema.get().getName();
        params[1] = ImmutableList.of(files);
        errors.add(new FirstPassValidationError(CheckLevel.FILE_LEVEL,
            "More than 1 file matching the file pattern: " + fileSchema.get().getPattern(),
            ValidationErrorCode.TOO_MANY_FILES_ERROR, params));
      }
    }
    return errors.build();
  }
}
