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
package org.icgc.dcc.submission.validation.first;

import static org.icgc.dcc.submission.validation.core.ErrorType.ErrorLevel.FILE_LEVEL;
import static org.icgc.dcc.submission.validation.core.ErrorType.ErrorLevel.ROW_LEVEL;
import static org.icgc.dcc.submission.validation.core.Validators.checkState;

import javax.validation.constraints.NotNull;

import lombok.NoArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.first.FileChecker.FileCheckers;
import org.icgc.dcc.submission.validation.first.RowChecker.RowCheckers;

import com.google.common.annotations.VisibleForTesting;

@Slf4j
@NoArgsConstructor
public class FirstPassValidator implements Validator {

  @NotNull
  private FileChecker fileChecker;
  @NotNull
  private RowChecker rowChecker;

  /**
   * For testing purposes only.
   */
  @VisibleForTesting
  FirstPassValidator(FileChecker fileChecker, RowChecker rowChecker) {
    this.fileChecker = fileChecker;
    this.rowChecker = rowChecker;
  }

  @Override
  public String getName() {
    return "First-pass Validator";
  }

  @Override
  public void validate(ValidationContext validationContext) {
    FileChecker fileChecker = this.fileChecker == null ?
        FileCheckers.getDefaultFileChecker(validationContext) :
        this.fileChecker;
    RowChecker rowChecker = this.rowChecker == null ?
        RowCheckers.getDefaultRowChecker(validationContext) :
        this.rowChecker;

    for (String filename : listRelevantFiles(validationContext)) {
      log.info("Validate '{}' level well-formedness for file: {}", FILE_LEVEL, filename);

      fileChecker.check(filename);
      checkState(getName());

      if (fileChecker.canContinue()) {
        log.info("Validating '{}' well-formedness for file: '{}'", ROW_LEVEL, filename);
        rowChecker.check(filename);
        checkState(getName());
      }
    }
  }

  private Iterable<String> listRelevantFiles(ValidationContext context) {
    val dictionary = context.getDictionary();
    val submissionDirectory = context.getSubmissionDirectory();
    val relevantPatterns = dictionary.getFilePatterns();

    return submissionDirectory.listFiles(relevantPatterns);
  }

}
