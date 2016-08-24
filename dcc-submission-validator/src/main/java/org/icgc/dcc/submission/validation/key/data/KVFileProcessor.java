/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.key.data;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static lombok.AccessLevel.PUBLIC;
import static org.icgc.dcc.common.core.util.Formats.formatCount;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.OPTIONAL_RELATION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.RELATION;
import static org.icgc.dcc.submission.validation.key.core.KVSubmissionProcessor.ROW_CHECKS_ENABLED;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.SpecialValue;
import org.icgc.dcc.common.hadoop.parser.FileRecordProcessor;
import org.icgc.dcc.submission.validation.key.core.KVDictionary;
import org.icgc.dcc.submission.validation.key.core.KVErrorType;
import org.icgc.dcc.submission.validation.key.core.KVFileParser;
import org.icgc.dcc.submission.validation.key.core.KVFileType;
import org.icgc.dcc.submission.validation.key.report.KVReporter;

import com.google.common.base.Optional;

@Slf4j
@RequiredArgsConstructor(access = PUBLIC)
public final class KVFileProcessor {

  /**
   * Constants.
   */
  private static final int DEFAULT_LOG_THRESHOLD = 1000000;

  /**
   * Helpers.
   */
  private final ValidationHelper valid = new ValidationHelper();
  private final SanityCheckHelper sanity = new SanityCheckHelper();

  /**
   * Meta data.
   */
  private final KVFileType fileType;
  private final Path filePath;

  @SneakyThrows
  public void processFile(
      final KVDictionary dictionary,
      final KVFileParser fileParser,
      final KVReporter reporter, // To report all but surjection errors at this point
      final KVPrimaryKeys primaryKeys, // FileType's primary keys
      final Map<KVFileType, KVReferencedPrimaryKeys> referencedPrimaryKeys, // Parent's primary keys
      final Map<KVFileType, KVEncounteredForeignKeys> encounteredKeys // Which parent keys are actually encountered.
                                                                      // Used for the future surjection check
  ) {
    log.info("{} - {}", fileType, filePath);

    val context = new KVRowContext(filePath.getName(), fileType, reporter,
        primaryKeys, encounteredKeys, referencedPrimaryKeys);

    fileParser.parse(filePath, new FileRecordProcessor<List<String>>() {

      @Override
      public void process(long lineNumber, List<String> record) {
        // Update the context
        val row = dictionary.getKeysIndices(fileType).getRow(record);
        context.nextRow(row, lineNumber);

        // Process the row
        log.debug("Row: '{}'", row);
        processRow(context, dictionary);
        processStatus(lineNumber);
      }

    });
  }

  /**
   * Processes a row (performs all validation except surjection).
   * <p>
   * TODO: very ugly, split per file type (subclass or compose), also for systems and referencing/non-referencing
   */
  private void processRow(KVRowContext context, KVDictionary dictionary) {
    val fileType = context.getFileType();
    // No uniqueness check for METH_ARRAY_P (at Vincent's request)
    if (hasPrimaryKeys(dictionary, fileType) && fileType != KVFileType.METH_ARRAY_P) { // TODO: Encode in the dictionary
      valid.validateUniqueness(context);
    }

    valid.validateForeignKeys(context);
    valid.validateOptionalForeignKeys(context);

    // E.g. Primary file types without secondary ones don't need to add their PKs as the surjection check will not be
    // performed for them.
    if (dictionary.hasChildren(fileType)) {
      addEncounteredPrimaryKey(context.getFileName(), context.getPrimaryKeys(), context.getRow());
    }

    if (!dictionary.getSurjectiveReferencedTypes(fileType).isEmpty()) {
      addEncounteredForeignKeys(context.getFileName(), context.getEncounteredKeys(), context.getRow());
    }
  }

  private void processStatus(long lineNumber) {
    if ((lineNumber % DEFAULT_LOG_THRESHOLD) == 0) {
      log.info("{} lines processed for '{}'", formatCount(lineNumber), filePath.getName());
    }
  }

  /**
   * For future relation checks.
   */
  private void addEncounteredPrimaryKey(String fileName, KVPrimaryKeys primaryKeys, KVRow row) {
    sanity.ensurePK(fileName, row);
    primaryKeys.updatePks(fileName, row);
  }

  /**
   * For future surjection check.
   */
  private void addEncounteredForeignKeys(String fileName, Map<KVFileType, KVEncounteredForeignKeys> encounteredKeys,
      KVRow row) {
    if (ROW_CHECKS_ENABLED) {
      checkState(!encounteredKeys.isEmpty(), "Encountered keys are expected to be present for type '%s'", fileType);
    }

    encounteredKeys.entrySet()
        .forEach(entry -> {
          KVFileType referencedFileType = entry.getKey();
          KVEncounteredForeignKeys encounteredFk = entry.getValue();
          sanity.ensureFk(fileName, row, referencedFileType);
          encounteredFk.addEncounteredForeignKey(row.getFk(referencedFileType));
        });
  }

  private static boolean hasPrimaryKeys(KVDictionary dictionary, KVFileType fileType) {
    return !dictionary.getPrimaryKeyNames(fileType).isEmpty();
  }

  /**
   * Encapsulate some validation behavior.
   */
  private class ValidationHelper {

    private void validateUniqueness(KVRowContext context) {
      sanity.ensurePK(context.getFileName(), context.getRow());

      val pk = context.getRow().getPk();
      if (context.getPrimaryKeys().containsPk(pk)) {
        context.getReporter().reportUniquenessError(context.getFileType(), context.getFileName(),
            context.getLineNumber(), pk);
      }
    }

    private void validateForeignKeys(KVRowContext context) {
      val referencedPks = context.getReferencedPrimaryKeys();
      val fks = context.getRow().getFks();
      for (val entry : fks.entrySet()) {
        val referencedFileType = entry.getKey();
        val fk = entry.getValue();
        val referencedPk = referencedPks.get(referencedFileType);

        validateForeignKey(context,
            referencedPk,
            fk,
            RELATION,
            referencedFileType);
      }
    }

    private void validateOptionalForeignKeys(KVRowContext context) {
      val row = context.getRow();
      if (row.hasOptionalFks()) {
        row.getOptionalFks().entrySet()
            .forEach(entry -> {
              KVFileType referencedFileType = entry.getKey();
              KVKey optionalFk = entry.getValue();
              KVReferencedPrimaryKeys referencedPks = context.getReferencedPrimaryKeys().get(referencedFileType);

              // DCC-3926: If any of the foreign key values are -888 then skip validation. This is because
              // optionallyReferencedPrimaryKeys.get().hasMatchingReference(fk) would not match anything because -888 is
              // not allowed for a primary key
                if (hasNotApplicableCode(optionalFk)) {
                  return;
                }
                validateForeignKey(context,
                    referencedPks,
                    optionalFk,
                    OPTIONAL_RELATION,
                    referencedFileType);
              });
      }
    }

    /**
     * Do not call directly outside of the inner class.
     */
    private void validateForeignKey(KVRowContext context,
        KVReferencedPrimaryKeys referencedPrimaryKeys,
        KVKey fk,
        KVErrorType errorType,
        KVFileType referencedFileType) {
      val fileType = context.getFileType();
      val fileName = context.getFileName();
      val lineNumber = context.getLineNumber();

      val foreignKeyViolation = !referencedPrimaryKeys.hasMatchingReference(fk);
      if (foreignKeyViolation) {
        switch (errorType) {
        case RELATION:
          context.getReporter().reportRelationError(fileType, fileName, lineNumber, fk,
              Optional.of(referencedFileType));
          break;
        case OPTIONAL_RELATION:
          context.getReporter().reportOptionalRelationError(fileType, fileName, lineNumber, fk,
              Optional.of(referencedFileType));
          break;
        default:
          throw new IllegalStateException(format("Invalid error type provided: '%s'", errorType));
        }
      }
    }

    private boolean hasNotApplicableCode(KVKey optionalFk) {
      for (val value : optionalFk.getValues()) {
        val present = SpecialValue.NOT_APPLICABLE_CODE.equals(value);
        if (present) {
          return true;
        }
      }

      return false;
    }
  }

  /**
   * Encapsulate some sanity checks behavior.
   */
  private class SanityCheckHelper {

    private final boolean active = ROW_CHECKS_ENABLED;

    private void ensurePK(String fileName, KVRow row) {
      if (!active) return;
      checkState(row.hasPk(),
          "Expecting to have a PK: '%s' ('%s')", row, fileName);
    }

    private void ensureFk(String fileName, KVRow row, KVFileType fileType) {
      if (!active) return;
      checkState(row.hasFk(fileType),
          "Expecting to have an FK: '%s' for type '%s' ('%s')", row, fileType, fileName);
    }

  }

}