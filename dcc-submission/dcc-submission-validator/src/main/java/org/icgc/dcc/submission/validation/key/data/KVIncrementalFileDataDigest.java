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
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.EXISTING_UNIQUE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.INCREMENTAL_UNIQUE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.PRIMARY_RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.SECONDARY_RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_P;

import java.util.Set;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.submission.validation.key.core.KVFileDescription;
import org.icgc.dcc.submission.validation.key.core.KVFileSystem;
import org.icgc.dcc.submission.validation.key.deletion.DeletionData;
import org.icgc.dcc.submission.validation.key.error.KVFileErrors;
import org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator;

import com.google.common.base.Optional;

public class KVIncrementalFileDataDigest extends KVFileDataDigest {

  private final KVFileSystem fileSystem;

  @SuppressWarnings("unused")
  private final DeletionData deletionData;

  private final KVFileDataDigest existingData;
  private final KVFileDataDigest existingReferencedData;
  private final Optional<KVFileDataDigest> optionalIncrementalReferencedData; // May not be re-submitted
  private final KVFileErrors errors;
  private final KVFileErrors surjectionErrors;
  private final SurjectivityValidator surjectivityValidator; // TODO: instantiate here?

  private final Set<KVKeyValues> surjectionEncountered = newTreeSet();

  /**
   * TODO: ! account for deletions (do not report errors for those)
   */
  public KVIncrementalFileDataDigest(
      KVFileDescription kvFileDescription, long logThreshold,

      @NonNull KVFileSystem fileSystem,

      @NonNull DeletionData deletionData,

      @NonNull KVFileDataDigest oldData,
      @NonNull KVFileDataDigest existingReferencedData,
      @NonNull Optional<KVFileDataDigest> optionalIncrementalReferencedData,

      @NonNull KVFileErrors errors,
      @NonNull KVFileErrors surjectionErrors, // TODO: better names (+explain)

      @NonNull SurjectivityValidator surjectivityValidator) {
    super(kvFileDescription, logThreshold);

    this.fileSystem = fileSystem;
    this.deletionData = deletionData;
    this.existingData = oldData;
    this.existingReferencedData = existingReferencedData;
    this.optionalIncrementalReferencedData = optionalIncrementalReferencedData;
    this.errors = errors;
    this.surjectionErrors = surjectionErrors;
    this.surjectivityValidator = surjectivityValidator;
  }

  /**
   * In the case of incremental data, the processing consists of validating it.
   */
  @Override
  protected void processTuple(KVTuple tuple, long lineCount) {
    checkState(kvFileDescription.getSubmissionType().isIncrementalData(), "TODO");

    // Clinical
    val fileType = kvFileDescription.getFileType();
    if (fileType == DONOR) { // TODO: split per file type (subclass or compose)

      // Uniqueness check against existing data
      if (existingData.pksContains(tuple.getPk())) {
        errors.addError(lineCount, EXISTING_UNIQUE, tuple.getPk());
        return;
      }

      // Uniqueness check against incremental data
      else if (pks.contains(tuple.getPk())) {
        errors.addError(lineCount, INCREMENTAL_UNIQUE, tuple.getPk());
        return;
      }

      // Valid data
      else {
        updatePksIfApplicable(tuple);
        checkState(!tuple.hasFk()); // Hence no surjection
        checkState(!tuple.hasSecondaryFk());
      }
    } else if (fileType == SPECIMEN) {

      // Uniqueness check against existing data
      if (existingData.pksContains(tuple.getPk())) {
        errors.addError(lineCount, EXISTING_UNIQUE, tuple.getPk());
        return; // TODO: do we want to report more errors all at once?
      }

      // Uniqueness check against incremental data
      else if (pks.contains(tuple.getPk())) {
        errors.addError(lineCount, INCREMENTAL_UNIQUE, tuple.getPk());
        return;
      }

      // Foreign key check
      else if (!hasMatchingReference(tuple.getFk())) {
        errors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
        return;
      }

      // Valid data
      else {
        updatePksIfApplicable(tuple);
        surjectionEncountered.add(checkNotNull(tuple.getFk()));
        checkState(!tuple.hasSecondaryFk());
      }
    } else if (fileType == SAMPLE) {

      // Uniqueness check against existing data
      if (existingData.pksContains(tuple.getPk())) {
        errors.addError(lineCount, EXISTING_UNIQUE, tuple.getPk());
        return;
      }

      // Uniqueness check against incremental data
      else if (pks.contains(tuple.getPk())) {
        errors.addError(lineCount, INCREMENTAL_UNIQUE, tuple.getPk());
        return;
      }

      // Foreign key check
      else if (!hasMatchingReference(tuple.getFk())) {
        errors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
        return;
      }

      // Valid data
      else {
        updatePksIfApplicable(tuple);
        surjectionEncountered.add(checkNotNull(tuple.getFk()));
        checkState(!tuple.hasSecondaryFk());
      }
    }

    // Ssm
    else if (fileType == SSM_M) {
      // TODO: later on, report on diff using: oldData.pksContains(tuple.getPk())

      // Uniqueness check against incremental data
      if (pks.contains(tuple.getPk())) {
        errors.addError(lineCount, INCREMENTAL_UNIQUE, tuple.getPk());
        return;
      }

      // Foreign key check
      else if (!hasMatchingReference(tuple.getFk())) {
        errors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
        return;
      }

      // Secondary foreign key check
      else if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK

        errors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());
        return;
      }

      // Valid data
      else {
        updatePksIfApplicable(tuple);
        surjectionEncountered.add(checkNotNull(tuple.getFk()));
        if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
          surjectionEncountered.add(tuple.getSecondaryFk());
        }
      }
    } else if (fileType == SSM_P) {
      ; // No uniqueness check for ssm_p

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        errors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
        return;
      }

      // Valid data
      else {
        checkState(!tuple.hasPk(), "TODO");
        surjectionEncountered.add(checkNotNull(tuple.getFk()));
        checkState(!tuple.hasSecondaryFk());
      }
    }

    // Cnsm
    else if (fileType == CNSM_M) {

      // Uniqueness check against incremental data
      if (pks.contains(tuple.getPk())) {
        errors.addError(lineCount, INCREMENTAL_UNIQUE, tuple.getPk());
        return;
      }

      // Foreign key check
      else if (!hasMatchingReference(tuple.getFk())) {
        errors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
        return;
      }

      // Secondary foreign key check
      else if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        errors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());
        return;
      }

      // Valid data
      else {
        updatePksIfApplicable(tuple);
        surjectionEncountered.add(checkNotNull(tuple.getFk()));
        if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
          surjectionEncountered.add(tuple.getSecondaryFk());
        }
      }
    } else if (fileType == CNSM_P) {

      // Uniqueness check against incremental data
      if (pks.contains(tuple.getPk())) {
        errors.addError(lineCount, INCREMENTAL_UNIQUE, tuple.getPk());
        return;
      }

      // Foreign key check
      else if (!hasMatchingReference(tuple.getFk())) {
        errors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
        return;
      }

      // Valid data
      else {
        updatePksIfApplicable(tuple);
        surjectionEncountered.add(checkNotNull(tuple.getFk()));
        checkState(!tuple.hasSecondaryFk());
      }
    } else if (fileType == CNSM_S) {
      ; // No uniqueness check for cnsm_s

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        errors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
        return;
      }

      // Valid data
      else {
        checkState(!tuple.hasPk(), "TODO");
        ; // No surjection between secondary and primary
        checkState(!tuple.hasSecondaryFk());
      }
    }
  }

  /**
   * @param fk May be primary or secondary FK.
   */
  private boolean hasMatchingReference(KVKeyValues fk) {
    return existingReferencedData.pksContains(fk)
        || (
        optionalIncrementalReferencedData.isPresent()
        && optionalIncrementalReferencedData.get().pksContains(fk));
  }

  @Override
  protected void postProcessing() {
    // Surjectivity; TODO: externalize

    val fileType = kvFileDescription.getFileType();
    if (fileType.hasSimpleSurjectiveRelation()) {
      surjectivityValidator
          .validateSimpleSurjection(
              fileType,
              !fileType.isReplaceAll() || fileSystem.hasIncrementalClinicalData() ? existingReferencedData : optionalIncrementalReferencedData
                  .get(), // FIXME
              surjectionErrors,
              surjectionEncountered);
    }

    if (fileType.hasComplexSurjectiveRelation()) {
      surjectivityValidator.addEncounteredSamples(surjectionEncountered);
    }
  }

}