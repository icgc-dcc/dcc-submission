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
package org.icgc.dcc.submission.validation.first.file;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.copyOf;
import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.RELATION_FILE_ERROR;
import static org.icgc.dcc.submission.core.report.ErrorType.REVERSE_RELATION_FILE_ERROR;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.FileSchemaRole;
import org.icgc.dcc.submission.dictionary.model.Relation;
import org.icgc.dcc.submission.validation.first.core.FileChecker;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

@Slf4j
public class FileReferenceChecker extends DelegatingFileChecker {

  public FileReferenceChecker(FileChecker compositeChecker) {
    this(compositeChecker, false);
  }

  public FileReferenceChecker(FileChecker compositeChecker, boolean failFast) {
    super(compositeChecker, failFast);
  }

  @Override
  public void performSelfCheck(String fileName) {
    log.info("Checking referenced file presence");
    referencedCheck(fileName);

    log.info("Checking referencing file presence");
    referencingCheck(fileName);
  }

  /**
   * Checks incoming references defined in the {@code FileSchema} associated with {@code fileName}.
   * 
   * @param fileName the file to check
   */
  private void referencedCheck(String fileName) {
    val fileSchema = getFileSchema(fileName);
    val relations = fileSchema.getRelations();
    log.info("Checking presence for '{}' referenced schemata: '{}'", relations.size(),
        copyOf(Iterables.transform(relations, new Function<Relation, String>() {

          @Override
          public String apply(Relation relation) {
            return relation.getOther();
          }
        })));

    for (val relation : relations) {
      val optionalReferencedFileSchema = getDictionary().getFileSchemaByName(relation.getOther());
      checkState(optionalReferencedFileSchema.isPresent(), "Invalid file schema: '%s'", relation.getOther());
      val referencedFileSchema = optionalReferencedFileSchema.get();
      if (referencedFileSchema.getRole() == FileSchemaRole.SUBMISSION) {
        val pattern = referencedFileSchema.getPattern();
        val fileNames = getFileSystem().getMatchingFileNames(pattern);
        if (fileNames.isEmpty()) {
          log.info("Fail referenced check for '{}': missing referencing file with schema '{}'",
              fileName, referencedFileSchema.getName());

          reportError(error()
              .fileName(fileName)
              .type(RELATION_FILE_ERROR)
              .params(referencedFileSchema.getName())
              .build());
        }
      } else {
        log.info("Skipping check for system file presence");
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
    val referencingFileSchemata = fileSchema.getIncomingSurjectiveRelationFileSchemata(getDictionary());
    log.info("Checking presence for '{}' referencing schemata: '{}'", referencingFileSchemata.size(),
        copyOf(Iterables.transform(referencingFileSchemata, new Function<FileSchema, String>() {

          @Override
          public String apply(FileSchema fileSchema) {
            return fileSchema.getName();
          }

        })));

    for (val referencingFileSchema : referencingFileSchemata) {
      checkState(referencingFileSchema.getRole() == FileSchemaRole.SUBMISSION);
      val referencingFileNames = getFileSystem().getMatchingFileNames(referencingFileSchema.getPattern());
      if (referencingFileNames.isEmpty()) {
        log.info("Fail referencing check for '{}': missing referencing file with schema '{}'",
            fileName, referencingFileSchema.getName());

        reportError(error()
            .fileName(fileName)
            .type(REVERSE_RELATION_FILE_ERROR)
            .params(referencingFileSchema.getName())
            .build());
      }
    }
  }

}
