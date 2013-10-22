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
import java.util.Map;
import java.util.regex.Pattern;

import lombok.val;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.icgc.dcc.submission.checker.Util.CheckLevel;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.ValidationErrorCode;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class ReferentialFileChecker extends CompositeFileChecker {

  private Map<String, String> cachedFileNames;
  private final boolean isFailFast;

  public ReferentialFileChecker(CompositeFileChecker compositeChecker) {
    this(compositeChecker, false);
  }

  public ReferentialFileChecker(CompositeFileChecker compositeChecker, boolean isFailFast) {
    super(compositeChecker);
    this.isFailFast = false;
    cacheFileSchemaNames();
  }

  @Override
  public List<FirstPassValidationError> selfCheck(String filePathname) {
    Builder<FirstPassValidationError> errors = ImmutableList.builder();
    errors.addAll(referencedCheck(filePathname));
    errors.addAll(referencingCheck(filePathname));
    return errors.build();
  }

  private List<FirstPassValidationError> referencedCheck(String filePathname) {
    List<FirstPassValidationError> errors = Lists.newLinkedList();
    Optional<FileSchema> fileSchema = getDictionary().fileSchema(cachedFileNames.get(filePathname));
    if (fileSchema.isPresent()) {
      for (val relation : fileSchema.get().getRelations()) {
        // fileSchema.get().getBidirectionalAfferentFileSchemata(dict);
        Optional<FileSchema> otherFileSchema = getDictionary().fileSchema(relation.getOther());
        if (otherFileSchema.isPresent()) {
          if (Lists
              .newArrayList(getSubmissionDirectory().listFile(Pattern.compile(otherFileSchema.get().getPattern())))
              .size() == 0) {
            errors.add(new FirstPassValidationError(getCheckLevel(), "Fail referenced check: missing referenced file ("
                + relation.getOther(), ValidationErrorCode.RELATION_FILE_ERROR));
          }
        }
      }
    }
    return errors;
  }

  private List<FirstPassValidationError> referencingCheck(String filePathname) {
    List<FirstPassValidationError> errors = Lists.newLinkedList();
    Optional<FileSchema> fileSchema = getDictionary().fileSchema(cachedFileNames.get(filePathname));
    if (fileSchema.isPresent()) {
      ;
      for (val otherFileSchema : fileSchema.get().getBidirectionalAfferentFileSchemata(getDictionary())) {
        if (Lists.newArrayList(getSubmissionDirectory().listFile(Pattern.compile(otherFileSchema.getPattern()))).size() == 0) {
          errors.add(new FirstPassValidationError(getCheckLevel(), "Fail referencing check: missing referencing file ("
              + otherFileSchema.getName(), ValidationErrorCode.REVERSE_RELATION_FILE_ERROR));
        }
      }
    }
    return errors;
  }

  @Override
  public CheckLevel getCheckLevel() {
    return CheckLevel.FILE_LEVEL;
  }

  @Override
  public boolean isFailFast() {
    return isFailFast;
  }

  private void cacheFileSchemaNames() {
    cachedFileNames = Maps.newHashMap();
    for (String filename : getSubmissionDirectory().listFile()) {
      for (FileSchema schema : getDictionary().getFiles()) {
        if (Pattern.matches(schema.getPattern(), filename)) {
          cachedFileNames.put(filename, schema.getName());
        }
      }
    }
  }
}
