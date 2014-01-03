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
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.SECONDARY_RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.UNIQUE_NEW;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.UNIQUE_ORIGINAL;
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

import org.icgc.dcc.submission.validation.key.KVFileDescription;
import org.icgc.dcc.submission.validation.key.deletion.DeletionData;
import org.icgc.dcc.submission.validation.key.error.KVFileErrors;
import org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator;

import com.google.common.collect.Sets;

public class KVIncrementalFileDataDigest extends KVFileDataDigest {

  @SuppressWarnings("unused")
  private final DeletionData deletionData;

  private final KVFileDataDigest oldData;
  private final KVFileDataDigest oldReferencedData;
  private final KVFileDataDigest newReferencedData;
  private final KVFileErrors errors;
  private final KVFileErrors surjectionErrors;
  private final SurjectivityValidator surjectivityValidator; // TODO: instantiate here?

  private final Set<KVKeyValues> surjectionEncountered = Sets.<KVKeyValues> newTreeSet();

  /**
   * TODO: ! account for deletions (do not report errors for those)
   */
  public KVIncrementalFileDataDigest(
      KVFileDescription kvFileDescription, long logThreshold,

      @NonNull DeletionData deletionData,

      @NonNull KVFileDataDigest oldData,
      @NonNull KVFileDataDigest oldReferencedData,
      KVFileDataDigest newReferencedData, // May be null

      @NonNull KVFileErrors errors,
      @NonNull KVFileErrors surjectionErrors, // TODO: better names (+explain)

      @NonNull SurjectivityValidator surjectivityValidator) {
    super(kvFileDescription, logThreshold);

    this.deletionData = deletionData;
    this.oldData = oldData;
    this.oldReferencedData = oldReferencedData;
    this.newReferencedData = newReferencedData;
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

    updatePksIfApplicable(tuple);

    // Clinical
    val fileType = kvFileDescription.getFileType();
    if (fileType == DONOR) { // TODO: split per file type (subclass or compose)

      // Uniqueness check against original data
      if (oldData.pksContains(tuple.getPk())) {
        errors.addError(lineCount, UNIQUE_ORIGINAL, tuple.getPk());
        return;
      }

      // Uniqueness check against new data
      else if (pks.contains(tuple.getPk())) {
        errors.addError(lineCount, UNIQUE_NEW, tuple.getPk());
        return;
      }

      // Valid data
      else {
        pks.add(checkNotNull(tuple.getPk()));
        checkState(!tuple.hasFk()); // Hence no surjection
        checkState(!tuple.hasSecondaryFk());
      }
    } else if (fileType == SPECIMEN) {

      // Uniqueness check against original data
      if (oldData.pksContains(tuple.getPk())) {
        errors.addError(lineCount, UNIQUE_ORIGINAL, tuple.getPk());
        return; // TODO: do we want to report more errors all at once?
      }

      // Uniqueness check against new data
      else if (pks.contains(tuple.getPk())) {
        errors.addError(lineCount, UNIQUE_NEW, tuple.getPk());
        return;
      }

      // Foreign key check
      else if (!oldReferencedData.pksContains(tuple.getFk())
          && !newReferencedData.pksContains(tuple.getFk())) {
        errors.addError(lineCount, RELATION, tuple.getFk());
        return;
      }

      // Valid data
      else {
        pks.add(checkNotNull(tuple.getPk()));
        surjectionEncountered.add(checkNotNull(tuple.getFk()));
        checkState(!tuple.hasSecondaryFk());
      }
    } else if (fileType == SAMPLE) {

      // Uniqueness check against original data
      if (oldData.pksContains(tuple.getPk())) {
        errors.addError(lineCount, UNIQUE_ORIGINAL, tuple.getPk());
        return;
      }

      // Uniqueness check against new data
      else if (pks.contains(tuple.getPk())) {
        errors.addError(lineCount, UNIQUE_NEW, tuple.getPk());
        return;
      }

      // Foreign key check
      else if (!oldReferencedData.pksContains(tuple.getFk())
          && !newReferencedData.pksContains(tuple.getFk())) {
        errors.addError(lineCount, RELATION, tuple.getFk());
        return;
      }

      // Valid data
      else {
        pks.add(checkNotNull(tuple.getPk()));
        surjectionEncountered.add(checkNotNull(tuple.getFk()));
        checkState(!tuple.hasSecondaryFk());
      }
    }

    // Ssm
    else if (fileType == SSM_M) {

      // Uniqueness check against original data
      if (oldData.pksContains(tuple.getPk())) {
        errors.addError(lineCount, UNIQUE_ORIGINAL, tuple.getPk());
        return;
      }

      // Uniqueness check against new data
      else if (pks.contains(tuple.getPk())) {
        errors.addError(lineCount, UNIQUE_NEW, tuple.getPk());
        return;
      }

      // Foreign key check
      else if (!oldReferencedData.pksContains(tuple.getFk())
          && !newReferencedData.pksContains(tuple.getFk())) {
        errors.addError(lineCount, RELATION, tuple.getFk());
        return;
      }

      // Secondary foreign key check
      else if (tuple.hasSecondaryFk() // May not
          && (!oldReferencedData.pksContains(tuple.getSecondaryFk())
          && !newReferencedData.pksContains(tuple.getSecondaryFk()))) {
        errors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());
        return;
      }

      // Valid data
      else {
        pks.add(checkNotNull(tuple.getPk()));
        surjectionEncountered.add(checkNotNull(tuple.getFk()));
        if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
          surjectionEncountered.add(tuple.getSecondaryFk());
        }
      }
    } else if (fileType == SSM_P) {
      ; // No uniqueness check for ssm_p

      // Foreign key check
      if (!oldReferencedData.pksContains(tuple.getFk())
          && !newReferencedData.pksContains(tuple.getFk())) {
        errors.addError(lineCount, RELATION, tuple.getFk());
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

      // Uniqueness check against original data
      if (oldData.pksContains(tuple.getPk())) {
        errors.addError(lineCount, UNIQUE_ORIGINAL, tuple.getPk());
        return;
      }

      // Uniqueness check against new data
      else if (pks.contains(tuple.getPk())) {
        errors.addError(lineCount, UNIQUE_NEW, tuple.getPk());
        return;
      }

      // Foreign key check
      else if (!oldReferencedData.pksContains(tuple.getFk())
          && !newReferencedData.pksContains(tuple.getFk())) {
        errors.addError(lineCount, RELATION, tuple.getFk());
        return;
      }

      // Secondary foreign key check
      else if (tuple.hasSecondaryFk() // May not
          && (!oldReferencedData.pksContains(tuple.getSecondaryFk())
          && !newReferencedData.pksContains(tuple.getSecondaryFk()))) {
        errors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());
        return;
      }

      // Valid data
      else {
        pks.add(checkNotNull(tuple.getPk()));
        surjectionEncountered.add(checkNotNull(tuple.getFk()));
        if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
          surjectionEncountered.add(tuple.getSecondaryFk());
        }
      }
    } else if (fileType == CNSM_P) {

      // Uniqueness check against original data
      if (oldData.pksContains(tuple.getPk())) {
        errors.addError(lineCount, UNIQUE_ORIGINAL, tuple.getPk());
        return;
      }

      // Uniqueness check against new data
      else if (pks.contains(tuple.getPk())) {
        errors.addError(lineCount, UNIQUE_NEW, tuple.getPk());
        return;
      }

      // Foreign key check
      else if (!oldReferencedData.pksContains(tuple.getFk())
          && !newReferencedData.pksContains(tuple.getFk())) {
        errors.addError(lineCount, RELATION, tuple.getFk());
        return;
      }

      // Valid data
      else {
        pks.add(checkNotNull(tuple.getPk()));
        surjectionEncountered.add(checkNotNull(tuple.getFk()));
        checkState(!tuple.hasSecondaryFk());
      }
    } else if (fileType == CNSM_S) {
      ; // No uniqueness check for cnsm_s

      // Foreign key check
      if (!oldReferencedData.pksContains(tuple.getFk())
          && !newReferencedData.pksContains(tuple.getFk())) {
        errors.addError(lineCount, RELATION, tuple.getFk());
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

  @Override
  protected void postProcessing() {
    // Surjectivity; TODO: externalize

    val fileType = kvFileDescription.getFileType();
    if (fileType.hasSimpleSurjectiveRelation()) {
      surjectivityValidator.validateSimpleSurjection(
          fileType,
          oldReferencedData, newReferencedData,
          surjectionErrors,
          surjectionEncountered);
    }

    if (fileType.hasComplexSurjectiveRelation()) {
      surjectivityValidator.addEncounteredSamples(surjectionEncountered);
    }
  }
}