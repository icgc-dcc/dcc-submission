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
import static org.icgc.dcc.submission.validation.key.core.KVDictionary.getRow;
import static org.icgc.dcc.submission.validation.key.core.KVSubmissionProcessor.ROW_CHECKS_ENABLED;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.OPTIONAL_RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.RELATION1;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.RELATION2;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_G;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.JCN_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.JCN_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_ARRAY_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_ARRAY_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_ARRAY_SYSTEM;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_SEQ_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_SEQ_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.PEXP_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.PEXP_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SGV_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SGV_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_S;

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

  private static final int DEFAULT_LOG_THRESHOLD = 1000000;

  private final ValidationHelper valid = new ValidationHelper();
  private final SanityCheckHelper sanity = new SanityCheckHelper();
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

    fileParser.parse(filePath, new FileRecordProcessor<List<String>>() {

      @Override
      public void process(long lineNumber, List<String> record) {
        val row = getRow(fileType, record);
        log.debug("Row: '{}'", row);

        processRow(row, lineNumber, reporter, primaryKeys,
            optionallyReferencedPrimaryKeys1, optionallyReferencedPrimaryKeys2,
            optionalEncounteredKeys);

        if ((lineNumber % DEFAULT_LOG_THRESHOLD) == 0) {
          logProcessedLine(lineNumber, false);
        }
      }

    });
  }

  private void logProcessedLine(long lineCount, boolean finished) {
    log.info("'{}' lines processed" + (finished ? " (finished)" : ""), lineCount);
  }

  /**
   * Processes a row (performs all validation except surjection).
   * <p>
   * TODO: very ugly, split per file type (subclass or compose), also for systems and referencing/non-referencing
   */
  private void processRow(
      KVRow row,
      long lineCount,
      KVReporter reporter,
      KVPrimaryKeys primaryKeys,
      Optional<KVReferencedPrimaryKeys> optionallyReferencedPrimaryKeys1,
      Optional<KVReferencedPrimaryKeys> optionallyReferencedPrimaryKeys2,
      Optional<KVEncounteredForeignKeys> optionallyEncounteredKeys) {
    val fileName = filePath.getName();

    // ===========================================================================
    // CLINICAL:

    // DONOR:
    if (fileType == DONOR) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      ; // No foreign key checks for DONOR

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK1(row);
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      ; // No surjection check for DONOR
    }

    // ---------------------------------------------------------------------------
    // SPECIMEN:
    else if (fileType == SPECIMEN) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      addEncounteredForeignKey(fileName, optionallyEncounteredKeys, row);
    }

    // ---------------------------------------------------------------------------
    // SAMPLE:
    else if (fileType == SAMPLE) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      addEncounteredForeignKey(fileName, optionallyEncounteredKeys, row);
    }

    // ===========================================================================
    // SSM:

    // SSM_M:
    else if (fileType == SSM_M) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);
      if (row.hasOptionalFk()) {
        valid.validateOptionalForeignKey(fileName, lineCount,
            optionallyReferencedPrimaryKeys1, row.getOptionalFk(), reporter);
      }

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      ; // No surjection check for meta files
    }

    // ---------------------------------------------------------------------------
    // SSM_P:
    else if (fileType == SSM_P) {
      ; // No uniqueness check for SSM_P
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoPK(row);
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredForeignKey(fileName, optionallyEncounteredKeys, row);
    }

    // ===========================================================================
    // CNSM:

    // CNSM_M:
    else if (fileType == CNSM_M) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);
      if (row.hasOptionalFk()) {
        valid.validateOptionalForeignKey(fileName, lineCount,
            optionallyReferencedPrimaryKeys1, row.getOptionalFk(), reporter);
      }

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      ; // No surjection check for meta files
    }

    // ---------------------------------------------------------------------------
    // CNSM_P:
    else if (fileType == CNSM_P) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);
      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      addEncounteredForeignKey(fileName, optionallyEncounteredKeys, row);
    }

    // ---------------------------------------------------------------------------
    // CNSM_S:
    else if (fileType == CNSM_S) {
      ; // No uniqueness check for CNSM_S
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);
      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoPK(row);
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      ; // No surjection check for secondary files
    }

    // ===========================================================================
    // STSM:

    // STSM_M:
    else if (fileType == STSM_M) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);
      if (row.hasOptionalFk()) {
        valid.validateOptionalForeignKey(fileName, lineCount,
            optionallyReferencedPrimaryKeys1, row.getOptionalFk(), reporter);
      }

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      ; // No surjection check for meta files
    }

    // ---------------------------------------------------------------------------
    // STSM_P:
    else if (fileType == STSM_P) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);
      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      addEncounteredForeignKey(fileName, optionallyEncounteredKeys, row);
    }

    // ---------------------------------------------------------------------------
    // STSM_S:
    else if (fileType == STSM_S) {
      ; // No uniqueness check for STSM_S
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoPK(row);
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      ; // No surjection check for secondary files
    }

    // ===========================================================================
    // MIRNA:

    // MIRNA_M:
    else if (fileType == MIRNA_M) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      ; // No surjection check for meta files
    }

    // ---------------------------------------------------------------------------
    // MIRNA_P:
    else if (fileType == MIRNA_P) {
      ; // No uniqueness check for MIRNA_P (unlike for other types, the PK is on the secondary file for MIRNA)
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);
      if (ROW_CHECKS_ENABLED) {
        // sanity.ensureNoPK(row);
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      // Special case (not a PK per se)
      primaryKeys.updateMirnaPKeys(fileType, fileName, row);

      addEncounteredForeignKey(fileName, optionallyEncounteredKeys, row);
    }

    // ---------------------------------------------------------------------------
    // MIRNA_S:
    else if (fileType == MIRNA_S) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter); // Exceptional
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      ; // No surjection check for secondary files
    }

    // ===========================================================================
    // OLD METH:

    // METH_M:
    else if (fileType == METH_M) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);
      if (row.hasOptionalFk()) {
        valid.validateOptionalForeignKey(fileName, lineCount,
            optionallyReferencedPrimaryKeys1, row.getOptionalFk(), reporter);
      }

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      ; // No surjection check for meta files
    }

    // ---------------------------------------------------------------------------
    // METH_P:
    else if (fileType == METH_P) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);
      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      addEncounteredForeignKey(fileName, optionallyEncounteredKeys, row);
    }

    // ---------------------------------------------------------------------------
    // METH_S:
    else if (fileType == METH_S) {
      ; // No uniqueness check for METH_S
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoPK(row);
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      ; // No surjection check for secondary files
    }

    // ===========================================================================
    // METH ARRAY:

    // METH_ARRAY_M:
    else if (fileType == METH_ARRAY_M) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      ; // No surjection check for meta files
    }

    // ---------------------------------------------------------------------------
    // METH_ARRAY_SYSTEM:
    else if (fileType == METH_ARRAY_SYSTEM) {
      ; // We perform no validation on system files at the moment

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
    }

    // ---------------------------------------------------------------------------
    // METH_ARRAY_P:
    else if (fileType == METH_ARRAY_P) {
      ; // No uniqueness check for METH_ARRAY_P (at Vincent's request)
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);
      valid.validateForeignKey2(fileName, lineCount, optionallyReferencedPrimaryKeys2, row.getFk2(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoOptionalFK(row);
      }

      // Not checking for surjection
    }

    // ===========================================================================
    // METH SEQ:

    // METH_SEQ_M:
    else if (fileType == METH_SEQ_M) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      ; // No surjection check for meta files
    }

    // ---------------------------------------------------------------------------
    // METH_SEQ_P:
    else if (fileType == METH_SEQ_P) {
      ; // No uniqueness check for METH_SEQ_P
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoPK(row);
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      // TODO: surjection checks?
    }

    // ===========================================================================
    // EXP:

    // EXP_M:
    else if (fileType == EXP_M) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      ; // No surjection check for meta files
    }

    // ---------------------------------------------------------------------------
    // EXP_G:
    else if (fileType == EXP_G) {
      ; // No uniqueness check for EXP_G
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoPK(row);
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredForeignKey(fileName, optionallyEncounteredKeys, row);
    }

    // ===========================================================================
    // PEXP:

    // PEXP_M:
    else if (fileType == PEXP_M) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      ; // No surjection check for meta files
    }

    // ---------------------------------------------------------------------------
    // PEXP_P:
    else if (fileType == PEXP_P) {
      ; // No uniqueness check for PEXP_P
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoPK(row);
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredForeignKey(fileName, optionallyEncounteredKeys, row);
    }

    // ===========================================================================
    // JCN:

    // JCN_M:
    else if (fileType == JCN_M) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      ; // No surjection check for meta files
    }

    // ---------------------------------------------------------------------------
    // JCN_P:
    else if (fileType == JCN_P) {
      ; // No uniqueness check for JCN_P
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoPK(row);
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredForeignKey(fileName, optionallyEncounteredKeys, row);
    }

    // ===========================================================================
    // SGV:

    // SGV_M:
    else if (fileType == SGV_M) {
      valid.validateUniqueness(fileName, lineCount, primaryKeys, row, reporter);
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredPrimaryKey(fileName, primaryKeys, row);
      ; // No surjection check for meta files
    }

    // ---------------------------------------------------------------------------
    // SGV_P:
    else if (fileType == SGV_P) {
      ; // No uniqueness check for SGV_P
      valid.validateForeignKey1(fileName, lineCount, optionallyReferencedPrimaryKeys1, row.getFk1(), reporter);

      if (ROW_CHECKS_ENABLED) {
        sanity.ensureNoPK(row);
        sanity.ensureNoFK2(row);
        sanity.ensureNoOptionalFK(row);
      }

      addEncounteredForeignKey(fileName, optionallyEncounteredKeys, row);
    }
  }

  private void addEncounteredPrimaryKey(String fileName,
      KVPrimaryKeys primaryKeys, KVRow row) {
    if (ROW_CHECKS_ENABLED) sanity.ensurePK(fileName, row);
    primaryKeys.updatePks(fileName, row);
  }

  private void addEncounteredForeignKey(String fileName,
      Optional<KVEncounteredForeignKeys> optionallyEncounteredKeys, KVRow row) {
    if (ROW_CHECKS_ENABLED) {
      checkState(optionallyEncounteredKeys.isPresent(),
          "Encountered keys are expected to be present for type '%s'", fileType);
      sanity.ensureFK1(fileName, row);
    }
    optionallyEncounteredKeys.get()
        .addEncounteredForeignKey(row.getFk1()); // Always uses FK1 at the moment.
  }

  /**
   * Encapsulate some validation behavior.
   */
  private class ValidationHelper {

    private void validateUniqueness(
        String fileName, long lineCount,
        KVPrimaryKeys primaryKeys, KVRow row, KVReporter reporter) {
      if (ROW_CHECKS_ENABLED) sanity.ensurePK(fileName, row);
      val pk = row.getPk();
      if (primaryKeys.containsPk(pk)) {
        reporter.reportUniquenessError(fileType, fileName, lineCount, pk);
      }
    }

    private void validateForeignKey1(
        String fileName, long lineCount,
        Optional<KVReferencedPrimaryKeys> optionallyReferencedPrimaryKeys1, KVKey fk1, KVReporter reporter) {
      validateForeignKey(
          fileName, lineCount,
          optionallyReferencedPrimaryKeys1, fk1,
          reporter, RELATION1);
    }

    private void validateForeignKey2(
        String fileName, long lineCount,
        Optional<KVReferencedPrimaryKeys> optionallyReferencedPrimaryKeys2, KVKey fk2, KVReporter reporter) {
      validateForeignKey(
          fileName, lineCount,
          optionallyReferencedPrimaryKeys2, fk2,
          reporter, RELATION2);
    }

    private void validateOptionalForeignKey(
        String fileName, long lineCount,

        // Always uses the same as FK1 at the moment ("matched_sample_id" refers to the same sample ID as
        // "analyzed_sample_id" but it's optional)
        Optional<KVReferencedPrimaryKeys> optionallyReferencedPrimaryKeys1,

        KVKey optionalFk, KVReporter reporter) {

      validateForeignKey(
          fileName, lineCount,
          optionallyReferencedPrimaryKeys1, optionalFk,
          reporter, OPTIONAL_RELATION);
    }

    /**
     * Do not call directly outside of the inner class.
     */
    private void validateForeignKey(
        String fileName, long lineCount,
        Optional<KVReferencedPrimaryKeys> optionallyReferencedPrimaryKeys, KVKey fk,
        KVReporter reporter, KVErrorType errorType) {
      if (ROW_CHECKS_ENABLED) {
        checkState(
            optionallyReferencedPrimaryKeys.isPresent(),
            "Referenced PKs are expected to be present for type '%s'", fileType);
      }
      val foreignKeyViolation = !optionallyReferencedPrimaryKeys.get().hasMatchingReference(fk);
      if (foreignKeyViolation) {
        switch (errorType) {
        case RELATION1:
          reporter.reportRelation1Error(fileType, fileName, lineCount, fk);
          break;
        case RELATION2:
          reporter.reportRelation2Error(fileType, fileName, lineCount, fk);
          break;
        case OPTIONAL_RELATION:
          reporter.reportOptionalRelationError(fileType, fileName, lineCount, fk);
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

    private void ensurePK(String fileName, KVRow row) {
      checkState(row.hasPk(),
          "Expecting to have a PK: '%s' ('%s')", row, fileName);
    }

    private void ensureFK1(String fileName, KVRow row) {
      checkState(row.hasFk1(),
          "Expecting to have an FK1: '%s' ('%s')", row, fileName);
    }

    private void ensureNoPK(KVRow row) {
      checkState(!row.hasPk(),
          "Row is not expected to contain a PK for type '%s': '%s'", fileType, row);
    }

    private void ensureNoFK1(KVRow row) {
      checkState(!row.hasFk1(),
          "Row is not expected to contain an FK1 for type '%s': '%s'", fileType, row);
    }

    private void ensureNoFK2(KVRow row) {
      checkState(!row.hasFk2(),
          "Row is not expected to contain an FK2 for type '%s': '%s'", fileType, row);
    }

    private void ensureNoOptionalFK(KVRow row) {
      checkState(!row.hasOptionalFk(),
          "Row is not expected to contain a seconary FK for type '%s': '%s'", fileType, row);
    }
  }

}