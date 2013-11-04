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
package org.icgc.dcc.submission.validation.checker.step;

import static org.icgc.dcc.submission.validation.core.ErrorType.RELATION_FILE_ERROR;
import static org.icgc.dcc.submission.validation.core.ErrorType.REVERSE_RELATION_FILE_ERROR;

import java.util.List;
import java.util.regex.Pattern;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.checker.FileChecker;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * TODO: split in two
 */
@Slf4j
public class ReferentialFileChecker extends CompositeFileChecker {

  public ReferentialFileChecker(FileChecker compositeChecker) {
    this(compositeChecker, false);
  }

  public ReferentialFileChecker(FileChecker compositeChecker, boolean failFast) {
    super(compositeChecker, failFast);
  }

  @Override
  public void performSelfCheck(String filename) {
    referencedCheck(filename);
    referencingCheck(filename);
  }

  private void referencedCheck(String filename) {
    Optional<FileSchema> fileSchema = getDictionary().fileSchema(getFileSchemaName(filename));
    if (fileSchema.isPresent()) {
      for (val relation : fileSchema.get().getRelations()) {
        Optional<FileSchema> otherFileSchema = getDictionary().fileSchema(relation.getOther());
        if (otherFileSchema.isPresent()) {
          String pattern = otherFileSchema.get().getPattern();
          List<String> files = getFiles(pattern);
          if (files.size() == 0) {
            log.info("Fail referenced check: missing referenced file (" + relation.getOther());

            incrementCheckErrorCount();
            getValidationContext().reportError(
                filename,
                RELATION_FILE_ERROR,
                fileSchema.get().getName());
          }
        }
      }
    }
  }

  private void referencingCheck(String filename) {
    Optional<FileSchema> fileSchema = getDictionary().fileSchema(getFileSchemaName(filename));

    if (fileSchema.isPresent()) {
      for (val otherFileSchema : fileSchema.get().getBidirectionalAfferentFileSchemata(getDictionary())) {
        List<String> files = getFiles(otherFileSchema.getPattern());
        if (files.size() == 0) {
          log.info("Fail referencing check: missing referencing file (" + otherFileSchema.getName());

          incrementCheckErrorCount();
          getValidationContext().reportError(
              filename,
              REVERSE_RELATION_FILE_ERROR,
              fileSchema.get().getName());
        }
      }
    }
  }

  private List<String> getFiles(String pattern) {
    return ImmutableList.copyOf(
        getSubmissionDirectory().listFile(Pattern.compile(pattern)));
  }

}
