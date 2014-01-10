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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newTreeSet;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.PRIMARY_RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.SECONDARY_RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.UNIQUENESS;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_G;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.JCN_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.JCN_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_S;
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

import java.util.Set;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.core.KVFileDescription;
import org.icgc.dcc.submission.validation.key.core.KVFileSystem;
import org.icgc.dcc.submission.validation.key.deletion.DeletionData;
import org.icgc.dcc.submission.validation.key.error.KVFileErrors;
import org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator;

import com.google.common.base.Optional;

@Slf4j
public class KVIncrementalFileDataDigest extends KVFileDataDigest {

  @SuppressWarnings("unused")
  private final KVFileSystem fileSystem;

  @SuppressWarnings("unused")
  private final DeletionData deletionData;

  // private final KVFileDataDigest existingData;
  // private final KVFileDataDigest existingReferencedData;
  // private final Optional<KVFileDataDigest> optionalIncrementalReferencedData; // May not be re-submitted

  private final Optional<KVFileDataDigest> optionalReferencedData;

  private final KVFileErrors fileErrors; // To collect all but surjection errors
  private final Optional<KVFileErrors> optionalReferencedFileErrors; // To collect simple surjection errors (complex
                                                                     // ones are collected later)

  private final SurjectivityValidator surjectivityValidator; // TODO: instantiate here?

  private final Set<KVKeyValues> encounteredKeys = newTreeSet();

  /**
   * TODO: ! account for deletions (do not report errors for those)
   */
  public KVIncrementalFileDataDigest(
      KVFileDescription kvFileDescription, long logThreshold,

      @NonNull KVFileSystem fileSystem,

      @NonNull DeletionData deletionData,

      @NonNull Optional<KVFileDataDigest> optionalReferencedData, // Not for DONOR for instance

      // @NonNull KVFileDataDigest oldData,
      // @NonNull KVFileDataDigest existingReferencedData,
      // @NonNull Optional<KVFileDataDigest> optionalIncrementalReferencedData,

      @NonNull KVFileErrors fileErrors,
      @NonNull Optional<KVFileErrors> optionalReferencedFileErrors,

      @NonNull SurjectivityValidator surjectivityValidator) {
    super(kvFileDescription, logThreshold);

    this.fileSystem = fileSystem;
    this.deletionData = deletionData;
    // this.existingData = oldData;
    // this.existingReferencedData = existingReferencedData;
    // this.optionalIncrementalReferencedData = optionalIncrementalReferencedData;
    this.optionalReferencedData = checkNotNull(optionalReferencedData);

    this.fileErrors = fileErrors;
    this.optionalReferencedFileErrors = optionalReferencedFileErrors;
    this.surjectivityValidator = surjectivityValidator;
  }

  /**
   * In the case of incremental data, the processing consists of validating it.
   */
  @Override
  protected void processTuple(KVTuple tuple, long lineCount) {
    if (TUPLE_CHECKS_ENABLED) checkState(kvFileDescription.getSubmissionType().isIncrementalData(), "TODO");

    // Clinical
    val fileType = kvFileDescription.getFileType();
    if (fileType == DONOR) { // TODO: split per file type (subclass or compose)

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // No foreign key check for DONOR

      updatePksIfApplicable(tuple);
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasFk()); // Hence no surjection
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    } else if (fileType == SPECIMEN) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    } else if (fileType == SAMPLE) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());
      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // SSM
    else if (fileType == SSM_M) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == SSM_P) {
      ; // No uniqueness check for SSM_P

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // CNSM
    else if (fileType == CNSM_M) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == CNSM_P) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    } else if (fileType == CNSM_S) {
      ; // No uniqueness check for CNSM

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      ; // No surjection between secondary and primary
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // STSM
    else if (fileType == STSM_M) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == STSM_P) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    } else if (fileType == STSM_S) {
      ; // No uniqueness check for STSM_s

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      ; // No surjection between secondary and primary
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // MIRNA
    else if (fileType == MIRNA_M) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == MIRNA_P) {
      ; // No uniqueness check for MIRNA_P (unlike for other types, the PK is on the secondary file for MIRNA)

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    } else if (fileType == MIRNA_S) {

      // Uniqueness check (unlike for other types, the PK is on the secondary file for MIRNA)
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      ; // No surjection between secondary and primary
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // METH
    else if (fileType == METH_M) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == METH_P) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    } else if (fileType == METH_S) {
      ; // No uniqueness check for METH_s

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      ; // No surjection between secondary and primary
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // EXP
    else if (fileType == EXP_M) {
      // TODO: later on, report on diff using: oldData.pksContains(tuple.getPk())

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK

        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == EXP_G) {
      ; // No uniqueness check for EXP_P

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // PEXP
    else if (fileType == PEXP_M) {
      // TODO: later on, report on diff using: oldData.pksContains(tuple.getPk())

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());
      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == PEXP_P) {
      ; // No uniqueness check for PEXP_P

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // JCN
    else if (fileType == JCN_M) {
      // TODO: later on, report on diff using: oldData.pksContains(tuple.getPk())

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());
      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == JCN_P) {
      ; // No uniqueness check for JCN_P

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // SGV
    else if (fileType == SGV_M) {
      // TODO: later on, report on diff using: oldData.pksContains(tuple.getPk())

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());
      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK

        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());
      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == SGV_P) {
      ; // No uniqueness check for SGV_P

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }
  }

  /**
   * @param fk May be primary or secondary FK.
   */
  private boolean hasMatchingReference(KVKeyValues fk) {
    if (TUPLE_CHECKS_ENABLED) {
      checkState(optionalReferencedData.isPresent(), "TODO");
    }
    return optionalReferencedData.get().pksContains(fk);
  }

  @Override
  protected void postProcessing() {
    // Surjectivity; TODO: externalize?

    val fileType = kvFileDescription.getFileType();
    if (fileType.hasOutgoingSimpleSurjectiveRelation()) {
      log.info("Post-processing: simply surjectivity check");
      checkState(optionalReferencedData.isPresent(), "TODO");
      checkState(optionalReferencedFileErrors.isPresent(), "TODO");
      surjectivityValidator
          .validateSimpleSurjection(
              // !fileType.isReplaceAll() || fileSystem.hasIncrementalClinicalData() ? existingReferencedData :
              // optionalIncrementalReferencedData.get(), // FIXME
              surjectivityValidator.getSurjectionExpectedKeys(optionalReferencedData.get()),
              encounteredKeys,
              optionalReferencedFileErrors.get());
    }

    if (fileType.hasOutgoingComplexSurjectiveRelation()) {
      log.info("Post-processing: complex surjectivity addition");
      // Simply adding them all for now, actual validation will have to take place after all meta files have been read
      // (unlike for "simple" surjection check).
      surjectivityValidator.addEncounteredSampleKeys(encounteredKeys);
    }
  }
}