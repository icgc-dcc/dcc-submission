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
package org.icgc.dcc.submission.validation.kv.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newTreeSet;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVErrorType.RELATION;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVErrorType.SECONDARY_RELATION;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVErrorType.UNIQUE_NEW;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVErrorType.UNIQUE_ORIGINAL;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.SSM_P;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.CNSM_M_FKS1;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.CNSM_M_FKS2;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.CNSM_M_PKS;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.CNSM_P_FKS;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.CNSM_P_PKS;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.CNSM_S_FKS;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.DONOR_PKS;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.SAMPLE_FKS;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.SAMPLE_PKS;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.SPECIMEN_FKS;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.SPECIMEN_PKS;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.SSM_M_FKS1;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.SSM_M_FKS2;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.SSM_M_PKS;
import static org.icgc.dcc.submission.validation.kv.error.KVSubmissionErrors.SSM_P_FKS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Set;

import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.icgc.dcc.submission.validation.kv.KVConstants;
import org.icgc.dcc.submission.validation.kv.deletion.DeletionData;
import org.icgc.dcc.submission.validation.kv.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.kv.enumeration.KVSubmissionType;
import org.icgc.dcc.submission.validation.kv.error.KVFileErrors;
import org.icgc.dcc.submission.validation.kv.surjectivity.SurjectivityValidator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

@Slf4j
@RequiredArgsConstructor
public class KVFileDataDigest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  // TODO: encapsulate in other object?
  @SuppressWarnings("unused")
  // Used in toString()
  private final KVSubmissionType submissionType;
  @SuppressWarnings("unused")
  // Used in toString()
  private final KVFileType fileType;
  @SuppressWarnings("unused")
  // Used in toString()
  private final String path; // TODO: optional?
  @SuppressWarnings("unused")
  // Used in toString()
  private final boolean placeholder;

  @Getter
  private final Set<KVKeys> pks; // TODO: change to arrays?

  public static KVFileDataDigest getEmptyInstance(KVSubmissionType submissionType, KVFileType fileType) {
    return new KVFileDataDigest(
        submissionType,
        fileType,
        (String) null,
        true,
        Sets.<KVKeys> newTreeSet());
  }

  /**
   * TODO: ! account for deletions (do not report errors for those)
   */
  @SneakyThrows
  public KVFileDataDigest( // TODO: very ugly: split processing of new and old
      KVSubmissionType submissionType, KVFileType fileType, String path,
      DeletionData deletionData,
      KVFileDataDigest oldData, KVFileDataDigest oldReferencedData, KVFileDataDigest newReferencedData,
      KVFileErrors errors, KVFileErrors surjectionErrors, // TODO: better names (+explain)
      SurjectivityValidator surjectivityValidator,
      long logThreshold) {
    this.submissionType = checkNotNull(submissionType);
    this.fileType = checkNotNull(fileType);
    this.path = path; // TODO
    this.placeholder = false;
    this.pks = newTreeSet();

    log.info("{}", StringUtils.repeat("=", 75));
    log.info("{}", Joiner.on(", ").join(submissionType, fileType, path));

    // Prepare surjection info gathering
    Set<KVKeys> surjectionEncountered = submissionType.isIncrementalData() ? Sets.<KVKeys> newTreeSet() : null;

    // Read line by lines
    @Cleanup
    val reader = new BufferedReader(new FileReader(new File(path)));
    long lineCount = 0;
    for (String line; (line = reader.readLine()) != null;) {

      // TODO: add sanity check on header
      if (lineCount != 0 && !line.trim().isEmpty()) {
        val row = newArrayList(KVConstants.TAB_SPLITTER.split(line)); // TODO: optimize (use array)
        log.debug("\t" + row);

        val tuple = getTuple(fileType, row);
        log.debug("tuple: {}", tuple);

        // Original data (old); This should already be valid, nothing to check
        if (submissionType.isExistingData()) {
          if (tuple.hasPk()) {
            pks.add(tuple.getPk());
          } else {
            checkState(!fileType.hasPk(), "TODO");
          }
        }

        // Incremental data (new)
        else {

          // Clinical
          if (fileType == DONOR) { // TODO: split per file type (subclass or compose)

            // Uniqueness check against original data
            if (oldData.pksContains(tuple.getPk())) {
              errors.addError(lineCount, UNIQUE_ORIGINAL, tuple.getPk());
              continue;
            }

            // Uniqueness check against new data
            else if (pks.contains(tuple.getPk())) {
              errors.addError(lineCount, UNIQUE_NEW, tuple.getPk());
              continue;
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
              continue; // TODO: do we want to report more errors all at once?
            }

            // Uniqueness check against new data
            else if (pks.contains(tuple.getPk())) {
              errors.addError(lineCount, UNIQUE_NEW, tuple.getPk());
              continue;
            }

            // Foreign key check
            else if (!oldReferencedData.pksContains(tuple.getFk())
                && !newReferencedData.pksContains(tuple.getFk())) {
              errors.addError(lineCount, RELATION, tuple.getFk());
              continue;
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
              continue;
            }

            // Uniqueness check against new data
            else if (pks.contains(tuple.getPk())) {
              errors.addError(lineCount, UNIQUE_NEW, tuple.getPk());
              continue;
            }

            // Foreign key check
            else if (!oldReferencedData.pksContains(tuple.getFk())
                && !newReferencedData.pksContains(tuple.getFk())) {
              errors.addError(lineCount, RELATION, tuple.getFk());
              continue;
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
              continue;
            }

            // Uniqueness check against new data
            else if (pks.contains(tuple.getPk())) {
              errors.addError(lineCount, UNIQUE_NEW, tuple.getPk());
              continue;
            }

            // Foreign key check
            else if (!oldReferencedData.pksContains(tuple.getFk())
                && !newReferencedData.pksContains(tuple.getFk())) {
              errors.addError(lineCount, RELATION, tuple.getFk());
              continue;
            }

            // Secondary foreign key check
            else if (tuple.hasSecondaryFk() // May not
                && (!oldReferencedData.pksContains(tuple.getSecondaryFk())
                && !newReferencedData.pksContains(tuple.getSecondaryFk()))) {
              errors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());
              continue;
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
              continue;
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
              continue;
            }

            // Uniqueness check against new data
            else if (pks.contains(tuple.getPk())) {
              errors.addError(lineCount, UNIQUE_NEW, tuple.getPk());
              continue;
            }

            // Foreign key check
            else if (!oldReferencedData.pksContains(tuple.getFk())
                && !newReferencedData.pksContains(tuple.getFk())) {
              errors.addError(lineCount, RELATION, tuple.getFk());
              continue;
            }

            // Secondary foreign key check
            else if (tuple.hasSecondaryFk() // May not
                && (!oldReferencedData.pksContains(tuple.getSecondaryFk())
                && !newReferencedData.pksContains(tuple.getSecondaryFk()))) {
              errors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());
              continue;
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
              continue;
            }

            // Uniqueness check against new data
            else if (pks.contains(tuple.getPk())) {
              errors.addError(lineCount, UNIQUE_NEW, tuple.getPk());
              continue;
            }

            // Foreign key check
            else if (!oldReferencedData.pksContains(tuple.getFk())
                && !newReferencedData.pksContains(tuple.getFk())) {
              errors.addError(lineCount, RELATION, tuple.getFk());
              continue;
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
              continue;
            }

            // Valid data
            else {
              checkState(!tuple.hasPk(), "TODO");
              ; // No surjection between secondary and primary
              checkState(!tuple.hasSecondaryFk());
            }
          }
        }

      }
      lineCount++;
      if ((lineCount % logThreshold) == 0) {
        logProcessedLine(lineCount, false);
      }
    }
    logProcessedLine(lineCount, true);

    // Surjectivity; TODO: externalize
    if (submissionType.isIncrementalData()) {
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

  private KVTuple getTuple(KVFileType fileType, List<String> row) {
    KVKeys pk = null, fk1 = null, fk2 = null;

    // Clinical
    if (fileType == DONOR) {
      pk = KVKeys.from(row, DONOR_PKS);
      fk1 = KVKeys.NOT_APPLICABLE;
      fk2 = KVKeys.NOT_APPLICABLE;
    } else if (fileType == SPECIMEN) {
      pk = KVKeys.from(row, SPECIMEN_PKS);
      fk1 = KVKeys.from(row, SPECIMEN_FKS);
      fk2 = KVKeys.NOT_APPLICABLE;
    } else if (fileType == SAMPLE) {
      pk = KVKeys.from(row, SAMPLE_PKS);
      fk1 = KVKeys.from(row, SAMPLE_FKS);
      fk2 = KVKeys.NOT_APPLICABLE;
    }

    // Ssm
    else if (fileType == SSM_M) {
      pk = KVKeys.from(row, SSM_M_PKS);
      fk1 = KVKeys.from(row, SSM_M_FKS1);
      fk2 = KVKeys.from(row, SSM_M_FKS2); // TODO: handle case where value is null or a missing code
    } else if (fileType == SSM_P) {
      pk = KVKeys.NOT_APPLICABLE;
      fk1 = KVKeys.from(row, SSM_P_FKS);
      fk2 = KVKeys.NOT_APPLICABLE;
    }

    // Cnsm
    else if (fileType == CNSM_M) {
      pk = KVKeys.from(row, CNSM_M_PKS);
      fk1 = KVKeys.from(row, CNSM_M_FKS1);
      fk2 = KVKeys.from(row, CNSM_M_FKS2); // TODO: handle case where value is null or a missing code
    } else if (fileType == CNSM_P) {
      pk = KVKeys.from(row, CNSM_P_PKS);
      fk1 = KVKeys.from(row, CNSM_P_FKS);
      fk2 = KVKeys.NOT_APPLICABLE;
    } else if (fileType == CNSM_S) {
      pk = KVKeys.NOT_APPLICABLE;
      fk1 = KVKeys.from(row, CNSM_S_FKS);
      fk2 = KVKeys.NOT_APPLICABLE;
    }

    checkState(pk != null || fk1 != null, "TODO: '%s'", row);
    return new KVTuple(pk, fk1, fk2);
  }

  private void logProcessedLine(long lineCount, boolean finished) {
    log.info("'{}' lines processed" + (finished ? " (finished)" : ""), lineCount);
  }

  @Override
  public String toString() {
    return toJsonSummaryString();
  }

  @SneakyThrows
  public String toJsonSummaryString() {
    return "\n" + MAPPER
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(this); // TODO: show sample only (first and last 10 for instance) + excluding nulls
  }

  public boolean pksContains(@NonNull KVKeys keys) {// TODO: consider removing such time consuming checks?
    return pks.contains(keys);
  }
}