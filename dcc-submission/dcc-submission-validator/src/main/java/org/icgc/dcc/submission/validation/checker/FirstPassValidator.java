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

import static org.icgc.dcc.submission.validation.core.ErrorType.ErrorLevel.FILE_LEVEL;
import static org.icgc.dcc.submission.validation.core.ErrorType.ErrorLevel.ROW_LEVEL;

import java.util.regex.Pattern;

import lombok.NoArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.checker.FileChecker.FileCheckers;
import org.icgc.dcc.submission.validation.checker.RowChecker.RowCheckers;
import org.icgc.dcc.submission.validation.core.FileSchemaDirectory;
import org.icgc.dcc.submission.validation.service.ValidationContext;
import org.icgc.dcc.submission.validation.service.Validator;

@Slf4j
@NoArgsConstructor
public class FirstPassValidator implements Validator {

  @Override
  public void validate(ValidationContext validationContext) {
    val fileChecker = FileCheckers.getDefaultFileChecker(validationContext);
    val rowChecker = RowCheckers.getDefaultRowChecker(validationContext);

    for (String filename : validationContext.getSubmissionDirectory().listFile()) {
      String fileSchemaName = getFileSchemaName(validationContext.getDictionary(), filename);
      if (fileSchemaName != null) {
        log.info("Validate '{}' level well-formedness for file schema: {}", FILE_LEVEL, fileSchemaName);

        fileChecker.check(filename);
        if (fileChecker.canContinue()) {
          log.info("Validating '{}' well-formedness for file schema: '{}'", ROW_LEVEL, fileSchemaName);
          rowChecker.check(filename);
        }
      }
    }
  }

  /**
   * TODO: Move to proper {@link SubmissionDirectory} or {@link FileSchemaDirectory} abstraction.
   */
  private static String getFileSchemaName(Dictionary dictionary, String fileName) {
    for (val schema : dictionary.getFiles()) {
      if (Pattern.matches(schema.getPattern(), fileName)) {
        return schema.getName();
      }
    }

    return null;
  }

}
