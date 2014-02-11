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
package org.icgc.dcc.submission.validation.first.step;

import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.RELATION_FILE_ERROR;
import static org.icgc.dcc.submission.core.report.ErrorType.REVERSE_RELATION_FILE_ERROR;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.first.FileChecker;

@Slf4j
public class ReferentialFileChecker extends CompositeFileChecker {

  public ReferentialFileChecker(FileChecker compositeChecker) {
    this(compositeChecker, false);
  }

  public ReferentialFileChecker(FileChecker compositeChecker, boolean failFast) {
    super(compositeChecker, failFast);
  }

  @Override
  public void performSelfCheck(String fileName) {
    referencedCheck(fileName);
    referencingCheck(fileName);
  }

  /**
   * Checks incoming references defined in the {@code FileSchema} associated with {@code fileName}.
   * 
   * @param fileName the file to check
   */
  private void referencedCheck(String fileName) {
    val fileSchema = getFileSchema(fileName);
    for (val relation : fileSchema.getRelations()) {
      val otherFileSchema = getDictionary().getFileSchemaByName(relation.getOther());
      checkState(otherFileSchema.isPresent(), "Invalid file schema: '%s'", relation.getOther());
      val pattern = otherFileSchema.get().getPattern();
      val fileNames = getFs().getMatchingFileNames(pattern);
      if (fileNames.isEmpty()) {
        log.info("Fail referenced check for '{}': missing referencing file with schema '{}'",
            fileName, otherFileSchema.get().getName());

        incrementCheckErrorCount();

        getValidationContext().reportError(
            error()
                .fileName(fileName)
                .type(RELATION_FILE_ERROR)
                .params(fileSchema.getName())
                .build());
      }
    }
  }

  /**
   * Checks outgoing references defined in the {@code FileSchema} associated with {@code fileName}.
   * 
   * @param fileName the file to check
   */
  private void referencingCheck(String fileName) {
    val fileSchema = getFileSchema(fileName);
    for (val otherFileSchema : fileSchema.getBidirectionalAfferentFileSchemata(getDictionary())) {
      val fileNames = getFs().getMatchingFileNames(otherFileSchema.getPattern());
      if (fileNames.isEmpty()) {
        log.info("Fail referencing check for '{}': missing referencing file with schema '{}'",
            fileName, otherFileSchema.getName());

        incrementCheckErrorCount();

        getValidationContext().reportError(
            error()
                .fileName(fileName)
                .type(REVERSE_RELATION_FILE_ERROR)
                .params(otherFileSchema.getName())
                .build());
      }
    }
  }

}
