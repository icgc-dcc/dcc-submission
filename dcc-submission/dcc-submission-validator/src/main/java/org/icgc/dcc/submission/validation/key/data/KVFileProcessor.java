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
package org.icgc.dcc.submission.validation.key.data;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static lombok.AccessLevel.PUBLIC;
import static org.icgc.dcc.core.util.FormatUtils.formatCount;
import static org.icgc.dcc.submission.validation.key.core.KVDictionary.getRow;
import static org.icgc.dcc.submission.validation.key.core.KVSubmissionProcessor.ROW_CHECKS_ENABLED;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.OPTIONAL_RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.RELATION1;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.RELATION2;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.core.parser.FileRecordProcessor;
import org.icgc.dcc.submission.validation.key.core.KVFileParser;
import org.icgc.dcc.submission.validation.key.enumeration.KVErrorType;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.report.KVReporter;

import com.google.common.base.Optional;

/**
 * TODO: MUST split class on a per type basis
 */
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
      final KVFileParser fileParser,
      final KVReporter reporter, // To report all but surjection errors at this point
      final KVPrimaryKeys primaryKeys,
      final Optional<KVReferencedPrimaryKeys> optionallyReferencedPrimaryKeys1, // N/A for DONOR for instance
      final Optional<KVReferencedPrimaryKeys> optionallyReferencedPrimaryKeys2, // Applicable for METH_ARRAY_P but N/A
                                                                                // for SSM_P for instance
      final Optional<KVEncounteredForeignKeys> optionalEncounteredKeys // N/A for SSM_P for instance
  ) {
    log.info("{}", fileType, filePath);

    val context = new KVRowContext(filePath.getName(), fileType, reporter,
        primaryKeys,
        optionallyReferencedPrimaryKeys1, optionallyReferencedPrimaryKeys2,
        optionalEncounteredKeys);

    fileParser.parse(filePath, new FileRecordProcessor<List<String>>() {

      @Override
      public void process(long lineNumber, List<String> record) {
        // Update the context
        val row = getRow(fileType, record);
        context.nextRow(row, lineNumber);

        // Process the row
        log.debug("Row: '{}'", row);
        processRow(context);
        processStatus(lineNumber);
      }

    });
  }

  private void processStatus(long lineNumber) {
    if ((lineNumber % DEFAULT_LOG_THRESHOLD) == 0) {
      log.info("{} lines processed for '{}'", formatCount(lineNumber), filePath.getName());
    }
  }

  /**
   * Processes a row (performs all validation except surjection).
   * <p>
   * TODO: very ugly, split per file type (subclass or compose), also for systems and referencing/non-referencing
   */
  private void processRow(KVRowContext context) {
    // @formatter:off
    switch (fileType) {
    
    //
    // Clinical
    //
    
    // CORE:
    case DONOR:             processDonor(context); break;
    case SPECIMEN:          processGenericPrimary2(context); break;
    case SAMPLE:            processGenericPrimary2(context); break;
    
    // TODO: OPTIONAL?
    
    //
    // Feature Types
    //    
    
    // SSM:
    case SSM_M:             processGenericMeta1(context); break;
    case SSM_P:             processGenericPrimary1(context); break;
      
    // CNSM:
    case CNSM_M:            processGenericMeta1(context); break;
    case CNSM_P:            processGenericPrimary2(context); break;
    case CNSM_S:            processGenericSecondary(context); break;

    // STSM:
    case STSM_M:            processGenericMeta1(context); break;
    case STSM_P:            processGenericPrimary2(context); break;
    case STSM_S:            processGenericSecondary(context); break;

    // MIRNA:
    case MIRNA_M:           processGenericMeta2(context); break;
    case MIRNA_P:           processMirnaPrimary(context); break;
    case MIRNA_S:           processMirnaSecondary(context); break;

    // OLD METH:
    case METH_M:            processGenericMeta1(context); break;
    case METH_P:            processGenericPrimary2(context); break;
    case METH_S:            processGenericSecondary(context); break;
    
    // METH ARRAY:
    case METH_ARRAY_M:      processGenericMeta2(context); break;
    case METH_ARRAY_SYSTEM: processMethArraySystem(context); break;
    case METH_ARRAY_P:      processMethArrayPrimary(context); break;

    // METH SEQ:
    case METH_SEQ_M:        processGenericMeta2(context); break;
    case METH_SEQ_P:        processMethSeqPrimary(context); break;
    
    // EXP:
    case EXP_M:             processGenericMeta2(context); break;
    case EXP_G:             processGenericPrimary1(context); break;

    // PEXP:
    case PEXP_M:            processGenericMeta2(context); break;
    case PEXP_P:            processGenericPrimary1(context); break;

    // JCN:
    case JCN_M:             processGenericMeta2(context); break;
    case JCN_P:             processGenericPrimary1(context); break;

    // SGV:
    case SGV_M:             processGenericMeta2(context); break;
    case SGV_P:             processGenericPrimary1(context); break;

    // TODO: Remaining feature types
    
    default: break;
    // TODO: Throw exception
    }
    // @formatter:on
  }

  private void processDonor(KVRowContext context) {
    valid.validateUniqueness(context);
    ; // No foreign key checks for DONOR

    sanity.ensureNoFK1(context.getRow());
    sanity.ensureNoFK2(context.getRow());
    sanity.ensureNoOptionalFK(context.getRow());

    addEncounteredPrimaryKey(context.getFileName(), context.getPrimaryKeys(), context.getRow());
    ; // No surjection check for DONOR
  }

  private void processGenericMeta1(KVRowContext context) {
    valid.validateUniqueness(context);
    valid.validateForeignKey1(context);
    if (context.getRow().hasOptionalFk()) {
      valid.validateOptionalForeignKey(context);
    }

    sanity.ensureNoFK2(context.getRow());

    addEncounteredPrimaryKey(context.getFileName(), context.getPrimaryKeys(), context.getRow());
    ; // No surjection check for meta files
  }

  private void processGenericMeta2(KVRowContext context) {
    valid.validateUniqueness(context);
    valid.validateForeignKey1(context);

    sanity.ensureNoFK2(context.getRow());
    sanity.ensureNoOptionalFK(context.getRow());

    addEncounteredPrimaryKey(context.getFileName(), context.getPrimaryKeys(), context.getRow());
    ; // No surjection check for meta files
  }

  private void processGenericPrimary1(KVRowContext context) {
    ; // No uniqueness check for METH_SEQ_P
    valid.validateForeignKey1(context);

    sanity.ensureNoPK(context.getRow());
    sanity.ensureNoFK2(context.getRow());
    sanity.ensureNoOptionalFK(context.getRow());

    addEncounteredForeignKey(context.getFileName(), context.getOptionallyEncounteredKeys(), context.getRow());
  }

  private void processGenericPrimary2(KVRowContext context) {
    valid.validateUniqueness(context);
    valid.validateForeignKey1(context);

    sanity.ensureNoFK2(context.getRow());
    sanity.ensureNoOptionalFK(context.getRow());

    addEncounteredPrimaryKey(context.getFileName(), context.getPrimaryKeys(), context.getRow());
    addEncounteredForeignKey(context.getFileName(), context.getOptionallyEncounteredKeys(), context.getRow());
  }

  private void processGenericSecondary(KVRowContext context) {
    ; // No uniqueness check for METH_SEQ_P
    valid.validateForeignKey1(context);

    sanity.ensureNoPK(context.getRow());
    sanity.ensureNoFK2(context.getRow());
    sanity.ensureNoOptionalFK(context.getRow());

    ; // No surjection check for secondary files
  }

  private void processMirnaPrimary(KVRowContext context) {
    ; // No uniqueness check for MIRNA_P (unlike for other types, the PK is on the secondary file for MIRNA)
    valid.validateForeignKey1(context);

    // sanity.ensureNoPK(row);
    sanity.ensureNoFK2(context.getRow());
    sanity.ensureNoOptionalFK(context.getRow());

    // Special case (not a PK per se)
    context.getPrimaryKeys().updateMirnaPKeys(fileType, context.getFileName(), context.getRow());

    addEncounteredForeignKey(context.getFileName(), context.getOptionallyEncounteredKeys(), context.getRow());
  }

  private void processMirnaSecondary(KVRowContext context) {
    valid.validateUniqueness(context); // Exceptional
    valid.validateForeignKey1(context);

    sanity.ensureNoFK2(context.getRow());
    sanity.ensureNoOptionalFK(context.getRow());

    ; // No surjection check for secondary files
  }

  private void processMethArrayPrimary(KVRowContext context) {
    ; // No uniqueness check for METH_ARRAY_P (at Vincent's request)
    valid.validateForeignKey1(context);
    valid.validateForeignKey2(context);

    sanity.ensureNoOptionalFK(context.getRow());

    // Not checking for surjection
  }

  private void processMethArraySystem(KVRowContext context) {
    ; // We perform no validation on system files at the moment

    addEncounteredPrimaryKey(context.getFileName(), context.getPrimaryKeys(), context.getRow());
  }

  private void processMethSeqPrimary(KVRowContext context) {
    ; // No uniqueness check for METH_SEQ_P
    valid.validateForeignKey1(context);

    sanity.ensureNoPK(context.getRow());
    sanity.ensureNoFK2(context.getRow());
    sanity.ensureNoOptionalFK(context.getRow());
  }

  private void addEncounteredPrimaryKey(String fileName, KVPrimaryKeys primaryKeys, KVRow row) {
    sanity.ensurePK(fileName, row);
    primaryKeys.updatePks(fileName, row);
  }

  private void addEncounteredForeignKey(String fileName, Optional<KVEncounteredForeignKeys> optionallyEncounteredKeys,
      KVRow row) {
    if (ROW_CHECKS_ENABLED) {
      checkState(optionallyEncounteredKeys.isPresent(),
          "Encountered keys are expected to be present for type '%s'", fileType);
    }

    sanity.ensureFK1(fileName, row);
    optionallyEncounteredKeys.get()
        .addEncounteredForeignKey(row.getFk1()); // Always uses FK1 at the moment.
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

    private void validateForeignKey1(KVRowContext context) {
      validateForeignKey(context,

          context.getOptionallyReferencedPrimaryKeys1(),
          context.getRow().getFk1(),
          RELATION1);
    }

    private void validateForeignKey2(KVRowContext context) {
      validateForeignKey(context,

          context.getOptionallyReferencedPrimaryKeys2(),
          context.getRow().getFk2(),
          RELATION2);
    }

    private void validateOptionalForeignKey(KVRowContext context) {
      validateForeignKey(context,

          context.getOptionallyReferencedPrimaryKeys1(),
          context.getRow().getOptionalFk(),
          OPTIONAL_RELATION);
    }

    /**
     * Do not call directly outside of the inner class.
     * @param context TODO
     */
    private void validateForeignKey(KVRowContext context,
        Optional<KVReferencedPrimaryKeys> optionallyReferencedPrimaryKeys, KVKey fk, KVErrorType errorType) {
      val fileType = context.getFileType();
      val fileName = context.getFileName();
      val lineNumber = context.getLineNumber();

      if (ROW_CHECKS_ENABLED) {
        checkState(
            optionallyReferencedPrimaryKeys.isPresent(),
            "Referenced PKs are expected to be present for type '%s'", fileType);
      }

      val foreignKeyViolation = !optionallyReferencedPrimaryKeys.get().hasMatchingReference(fk);
      if (foreignKeyViolation) {
        switch (errorType) {
        case RELATION1:
          context.getReporter().reportRelation1Error(fileType, fileName, lineNumber, fk);
          break;
        case RELATION2:
          context.getReporter().reportRelation2Error(fileType, fileName, lineNumber, fk);
          break;
        case OPTIONAL_RELATION:
          context.getReporter().reportOptionalRelationError(fileType, fileName, lineNumber, fk);
          break;
        default:
          throw new IllegalStateException(format("Invalid error type provided: '%s'", errorType));
        }
      }
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

    private void ensureFK1(String fileName, KVRow row) {
      if (!active) return;
      checkState(row.hasFk1(),
          "Expecting to have an FK1: '%s' ('%s')", row, fileName);
    }

    private void ensureNoPK(KVRow row) {
      if (!active) return;
      checkState(!row.hasPk(),
          "Row is not expected to contain a PK for type '%s': '%s'", fileType, row);
    }

    private void ensureNoFK1(KVRow row) {
      if (!active) return;
      checkState(!row.hasFk1(),
          "Row is not expected to contain an FK1 for type '%s': '%s'", fileType, row);
    }

    private void ensureNoFK2(KVRow row) {
      if (!active) return;
      checkState(!row.hasFk2(),
          "Row is not expected to contain an FK2 for type '%s': '%s'", fileType, row);
    }

    private void ensureNoOptionalFK(KVRow row) {
      if (!active) return;
      checkState(!row.hasOptionalFk(),
          "Row is not expected to contain a seconary FK for type '%s': '%s'", fileType, row);
    }

  }

}