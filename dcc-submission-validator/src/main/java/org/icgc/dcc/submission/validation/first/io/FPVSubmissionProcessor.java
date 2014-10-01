/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.first.io;

import static com.google.common.base.Strings.repeat;
import static com.google.common.collect.Iterables.transform;
import static org.icgc.dcc.submission.core.report.ErrorLevel.FILE_LEVEL;
import static org.icgc.dcc.submission.core.report.ErrorLevel.ROW_LEVEL;
import static org.icgc.dcc.submission.validation.core.Validators.checkInterrupted;

import javax.validation.constraints.NotNull;

import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.core.FileChecker;
import org.icgc.dcc.submission.validation.first.core.FileCheckers;
import org.icgc.dcc.submission.validation.first.core.RowChecker;
import org.icgc.dcc.submission.validation.first.core.RowCheckers;

import com.google.common.base.Function;

/**
 * Main logic for the FPV.
 */
@Slf4j
@NoArgsConstructor
public class FPVSubmissionProcessor {

  /**
   * For tests only (TODO: change that...).
   */
  @NotNull
  @Setter
  private FileChecker fileChecker;
  @NotNull
  @Setter
  private RowChecker rowChecker;

  public void process(String stepName, ValidationContext validationContext, FPVFileSystem fs) {
    FileChecker fileChecker = this.fileChecker == null ?
        FileCheckers.getDefaultFileChecker(validationContext, fs) :
        this.fileChecker;
    RowChecker rowChecker = this.rowChecker == null ?
        RowCheckers.getDefaultRowChecker(validationContext, fs) :
        this.rowChecker;

    // TODO: Add check that at least DONOR exists (+ create new error)

    // Resolve the selected files to validate
    val fileNames = getSelectedFileNames(validationContext, fs);

    // Validate each file in turn
    for (val fileName : fileNames) {
      log.info(banner());
      log.info("Validate '{}' level well-formedness for file: {}", FILE_LEVEL, fileName);

      fileChecker.checkFile(fileName);
      checkInterrupted(stepName);

      if (fileChecker.canContinue()) {
        log.info("Validating '{}' well-formedness for file: '{}'", ROW_LEVEL, fileName);
        rowChecker.checkFile(fileName);
        checkInterrupted(stepName);
      }
    }
  }

  private static Iterable<String> getSelectedFileNames(ValidationContext validationContext, FPVFileSystem fs) {
    val selectedFilePatterns = getSelectedFilePatterns(validationContext);

    return fs.listMatchingSubmissionFiles(selectedFilePatterns);
  }

  private static Iterable<String> getSelectedFilePatterns(ValidationContext context) {
    val fileSchemata = context.getDictionary().getFileSchemata(context.getDataTypes());

    // Selective validation filtering
    return transform(fileSchemata, new Function<FileSchema, String>() {

      @Override
      public String apply(FileSchema fileSchema) {
        return fileSchema.getPattern();
      }

    });
  }

  private static String banner() {
    return repeat("=", 75);
  }

}
