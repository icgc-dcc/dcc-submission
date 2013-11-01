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

import static com.google.common.collect.Lists.newLinkedList;

import java.util.List;
import java.util.regex.Pattern;

import lombok.val;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ErrorCode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class ReferentialFileChecker extends CompositeFileChecker {

  public ReferentialFileChecker(FileChecker compositeChecker) {
    this(compositeChecker, false);
  }

  public ReferentialFileChecker(FileChecker compositeChecker, boolean failFast) {
    super(compositeChecker, failFast);
  }

  @Override
  public List<FirstPassValidationError> performSelfCheck(String filename) {
    Builder<FirstPassValidationError> errors = ImmutableList.builder();
    errors.addAll(referencedCheck(filename));
    errors.addAll(referencingCheck(filename));

    return errors.build();
  }

  private List<FirstPassValidationError> referencedCheck(String filename) {
    Builder<FirstPassValidationError> errors = ImmutableList.builder();
    Optional<FileSchema> fileSchema = getDictionary().fileSchema(getFileSchemaName(filename));
    if (fileSchema.isPresent()) {
      for (val relation : fileSchema.get().getRelations()) {
        Optional<FileSchema> otherFileSchema = getDictionary().fileSchema(relation.getOther());
        if (otherFileSchema.isPresent()) {
          List<String> files = ImmutableList.copyOf(
              getSubmissionDirectory().listFile(Pattern.compile(otherFileSchema.get().getPattern())));
          if (files.size() == 0) {
            errors.add(new FirstPassValidationError(getCheckLevel(),
                "Fail referenced check: missing referenced file (" + relation.getOther(),
                ErrorCode.RELATION_FILE_ERROR,
                new Object[] { fileSchema.get().getName() }, -1));
          }
        }
      }
    }

    return errors.build();
  }

  private List<FirstPassValidationError> referencingCheck(String filename) {
    List<FirstPassValidationError> errors = newLinkedList();
    Optional<FileSchema> fileSchema = getDictionary().fileSchema(getFileSchemaName(filename));

    if (fileSchema.isPresent()) {
      for (val otherFileSchema : fileSchema.get().getBidirectionalAfferentFileSchemata(getDictionary())) {
        List<String> files = getFiles(otherFileSchema);
        if (files.size() == 0) {
          errors
              .add(new FirstPassValidationError(getCheckLevel(), "Fail referencing check: missing referencing file ("
                  + otherFileSchema.getName(), ErrorCode.REVERSE_RELATION_FILE_ERROR, new Object[] { fileSchema.get()
                  .getName() }, -1));
        }
      }
    }

    return errors;
  }

  private ImmutableList<String> getFiles(FileSchema otherFileSchema) {
    return ImmutableList.copyOf(getSubmissionDirectory().listFile(Pattern.compile(otherFileSchema.getPattern())));
  }

}
