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
package org.icgc.dcc.submission.validation.kv;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newTreeSet;
import static org.icgc.dcc.submission.validation.kv.FileType.CNSM_M;
import static org.icgc.dcc.submission.validation.kv.FileType.CNSM_P;
import static org.icgc.dcc.submission.validation.kv.FileType.CNSM_S;
import static org.icgc.dcc.submission.validation.kv.FileType.DONOR;
import static org.icgc.dcc.submission.validation.kv.FileType.SAMPLE;
import static org.icgc.dcc.submission.validation.kv.FileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.kv.FileType.SSM_M;
import static org.icgc.dcc.submission.validation.kv.FileType.SSM_P;
import static org.icgc.dcc.submission.validation.kv.Helper.getPath;
import static org.icgc.dcc.submission.validation.kv.Helper.hasNewClinical;
import static org.icgc.dcc.submission.validation.kv.Helper.hasNewCnsm;
import static org.icgc.dcc.submission.validation.kv.Helper.hasNewSsm;
import static org.icgc.dcc.submission.validation.kv.Helper.hasOriginalClinical;
import static org.icgc.dcc.submission.validation.kv.Helper.hasOriginalCnsm;
import static org.icgc.dcc.submission.validation.kv.Helper.hasOriginalSsm;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.CNSM_M_FKS1;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.CNSM_M_FKS2;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.CNSM_M_PKS;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.CNSM_P_FKS;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.CNSM_P_PKS;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.CNSM_S_FKS;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.DONOR_PKS;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.SAMPLE_FKS;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.SAMPLE_PKS;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.SPECIMEN_FKS;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.SPECIMEN_PKS;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.SSM_M_FKS1;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.SSM_M_FKS2;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.SSM_M_PKS;
import static org.icgc.dcc.submission.validation.kv.KeyValidatorConstants.SSM_P_FKS;
import static org.icgc.dcc.submission.validation.kv.SubmissionType.NEW_FILE;
import static org.icgc.dcc.submission.validation.kv.SubmissionType.ORIGINAL_FILE;
import static org.icgc.dcc.submission.validation.kv.SubmissionType.TREATED_AS_ORIGINAL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.StringUtils;
import org.icgc.dcc.submission.validation.kv.Keys.Tuple;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

/**
 * Very primitive version. The non-genericity was a request from Bob.
 * <p>
 * TODO:<br/>
 * TO_BE_REMOVED<br/>
 * optional files<br/>
 * other feature types<br/>
 * consider removing the check* (costly)<br/>
 */
@Slf4j
@RequiredArgsConstructor
public class KeyValidator {

  private final long logThreshold;
  private final KeyValidatorData data = new KeyValidatorData();
  private final Surjectivity surjectivity = new Surjectivity();

  public void validate() {
    loadOriginalData();
    loadNewData();
    surjectivity.validateComplexSurjection(data);
    // TODO: report errors
    // TODO: combine for reporting on dropping of clinical (and others?)
    // TODO: combine for materialization
    log.info("done.");
  }

  private void loadOriginalData() {

    // Original clinical
    checkState(hasOriginalClinical(), "TODO");
    data.donorOriginalDigest = getFileDigest(ORIGINAL_FILE, DONOR, getPath(ORIGINAL_FILE, DONOR));
    data.specimenOriginalDigest = getFileDigest(ORIGINAL_FILE, SPECIMEN, getPath(ORIGINAL_FILE, SPECIMEN));
    data.sampleOriginalDigest = getFileDigest(ORIGINAL_FILE, SAMPLE, getPath(ORIGINAL_FILE, SAMPLE));
    log.info("{}", data.donorOriginalDigest);
    log.info("{}", data.specimenOriginalDigest);
    log.info("{}", data.sampleOriginalDigest);

    // Original ssm
    if (hasOriginalSsm()) {
      data.ssmMOriginalDigest = getFileDigest(ORIGINAL_FILE, SSM_M, getPath(ORIGINAL_FILE, SSM_M));
      // // TODO: May not have to load p?
      data.ssmPOriginalDigest = getFileDigest(ORIGINAL_FILE, SSM_P, getPath(ORIGINAL_FILE, SSM_P));
    } else {
      data.ssmMOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, SSM_M);
      data.ssmPOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, SSM_P);
    }
    log.info("{}", data.ssmMOriginalDigest);
    log.info("{}", data.ssmPOriginalDigest);

    // Original cnsm
    if (hasOriginalCnsm()) {
      data.cnsmMOriginalDigest = getFileDigest(ORIGINAL_FILE, CNSM_M, getPath(ORIGINAL_FILE, CNSM_M));
      data.cnsmPOriginalDigest = getFileDigest(ORIGINAL_FILE, CNSM_P, getPath(ORIGINAL_FILE, CNSM_P));
      // TODO: May not have to load s?
      data.cnsmSOriginalDigest = getFileDigest(ORIGINAL_FILE, CNSM_S, getPath(ORIGINAL_FILE, CNSM_S));
    } else {
      data.cnsmMOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, CNSM_M);
      data.cnsmPOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, CNSM_P);
      data.cnsmSOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, CNSM_S);
    }
    log.info("{}", data.cnsmMOriginalDigest);
    log.info("{}", data.cnsmPOriginalDigest);
    log.info("{}", data.cnsmSOriginalDigest);
  }

  /**
   * Order matters!
   */
  private void loadNewData() {

    // New clinical
    if (hasNewClinical()) {
      data.donorNewDigest = getFileDigest(TREATED_AS_ORIGINAL, DONOR, getPath(NEW_FILE, DONOR));
      log.info("{}", data.donorNewDigest);

      data.specimenNewDigest = getFileDigest(TREATED_AS_ORIGINAL, SPECIMEN, getPath(NEW_FILE, SPECIMEN));
      log.info("{}", data.specimenNewDigest);

      data.sampleNewDigest = getFileDigest(TREATED_AS_ORIGINAL, SAMPLE, getPath(NEW_FILE, SAMPLE));
      log.info("{}", data.sampleNewDigest);
    }

    // New ssm
    if (hasNewSsm()) {
      data.ssmMNewDigest = getFileDigest(NEW_FILE, SSM_M, getPath(NEW_FILE, SSM_M));
      data.ssmPNewDigest = getFileDigest(NEW_FILE, SSM_P, getPath(NEW_FILE, SSM_P));
    }
    log.info("{}", data.ssmMNewDigest);
    log.info("{}", data.ssmPNewDigest);

    // New cnsm
    if (hasNewCnsm()) {
      data.cnsmMNewDigest = getFileDigest(NEW_FILE, CNSM_M, getPath(NEW_FILE, CNSM_M));
      data.cnsmPNewDigest = getFileDigest(NEW_FILE, CNSM_P, getPath(NEW_FILE, CNSM_P));
      data.cnsmSNewDigest = getFileDigest(NEW_FILE, CNSM_S, getPath(NEW_FILE, CNSM_S));
    }
    log.info("{}", data.cnsmMNewDigest);
    log.info("{}", data.cnsmPNewDigest);
    log.info("{}", data.cnsmSNewDigest);
  }

  // TODO: move to file digest rather (as hierarchy)?
  @SneakyThrows
  private FileDigest getFileDigest(SubmissionType submissionType, FileType fileType, String path) {
    log.info("{}", StringUtils.repeat("=", 75));
    log.info("{}", Joiner.on(", ").join(submissionType, fileType, path));

    // TODO: use builder?
    Set<Keys> pks = newTreeSet();
    Set<Keys> fks = newTreeSet();
    Set<Keys> secondaryFks = newTreeSet();
    List<Tuple> tuples = newArrayList();

    // Prepare surjection info gathering
    Set<Keys> surjectionEncountered = submissionType.isIncrementalData() ? Sets.<Keys> newTreeSet() : null;

    // Read line by lines
    BufferedReader bufferedReader = // TODO: guava way
        new BufferedReader(new FileReader(new File(path)));
    String line = null;
    int lineCount = 0;
    while ((line = bufferedReader.readLine()) != null) {
      // TODO: add sanity check on header
      if (lineCount != 0 && !line.trim().isEmpty()) {
        List<String> row = newArrayList(Splitter.on('\t').split(line)); // TODO: optimize (use array)
        log.debug("\t" + row);

        val tuple = getTuple(fileType, row);
        log.debug("tuple: {}", tuple);
        if (submissionType.isExistingData()) { // This should already be valid, nothing to check
          if (tuple.hasPk()) {
            pks.add(tuple.getPk());
          }
          if (tuple.hasFk()) {
            fks.add(tuple.getFk());
          }
          if (tuple.hasSecondaryFk()) {
            secondaryFks.add(tuple.getSecondaryFk());
          }
          tuples.add(tuple); // At least one must be non-null
        }

        // Incremental data
        else {

          // Clinical
          if (fileType == DONOR) {

            // Uniqueness check against original data
            if (data.donorOriginalDigest.pksContains(tuple.getPk())) {
              data.donorUniqueOriginalErrors.add(tuple);
            }

            // Uniqueness check against new data
            else if (pks.contains(tuple.getPk())) {
              data.donorUniqueNewErrors.add(tuple);
            }

            // Valid data
            else {
              pks.add(checkNotNull(tuple.getPk()));
              checkState(!tuple.hasFk());
              checkState(!tuple.hasSecondaryFk());
            }
          } else if (fileType == SPECIMEN) {

            // Uniqueness check against original data
            if (data.specimenOriginalDigest.pksContains(tuple.getPk())) {
              data.specimenUniqueOriginalErrors.add(tuple);
            }

            // Uniqueness check against new data
            else if (pks.contains(tuple.getPk())) {
              data.specimenUniqueNewErrors.add(tuple);
            }

            // Foreign key check
            else if (!data.donorOriginalDigest.pksContains(tuple.getFk())
                && !data.donorNewDigest.pksContains(tuple.getFk())) {
              data.specimenRelationErrors.add(tuple);
            }

            // Valid data
            else {
              pks.add(checkNotNull(tuple.getPk()));
              surjectionEncountered.add(checkNotNull(tuple.getFk()));
              checkState(!tuple.hasSecondaryFk());
            }
          } else if (fileType == SAMPLE) {

            // Uniqueness check against original data
            if (data.sampleOriginalDigest.pksContains(tuple.getPk())) {
              data.sampleUniqueOriginalErrors.add(tuple);
            }

            // Uniqueness check against new data
            else if (pks.contains(tuple.getPk())) {
              data.sampleUniqueNewErrors.add(tuple);
            }

            // Foreign key check
            else if (!data.specimenOriginalDigest.pksContains(tuple.getFk())
                && !data.specimenNewDigest.pksContains(tuple.getFk())) {
              data.sampleRelationErrors.add(tuple);
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
            if (data.ssmMOriginalDigest.pksContains(tuple.getPk())) {
              data.ssmMUniqueOriginalErrors.add(tuple);
            }

            // Uniqueness check against new data
            else if (pks.contains(tuple.getPk())) {
              data.ssmMUniqueNewErrors.add(tuple);
            }

            // Foreign key check
            else if ((!data.sampleOriginalDigest.pksContains(tuple.getFk())
                && !data.sampleNewDigest.pksContains(tuple.getFk()))
                || (!data.sampleOriginalDigest.pksContains(tuple.getSecondaryFk())
                && !data.sampleNewDigest.pksContains(tuple.getSecondaryFk()))) {
              data.ssmMRelationErrors.add(tuple);
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
            if (!data.ssmMOriginalDigest.pksContains(tuple.getFk())
                && !data.ssmMNewDigest.pksContains(tuple.getFk())) {
              data.ssmPRelationErrors.add(tuple);
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
            if (data.cnsmMOriginalDigest.pksContains(tuple.getPk())) {
              data.cnsmMUniqueOriginalErrors.add(tuple);
            }

            // Uniqueness check against new data
            else if (pks.contains(tuple.getPk())) {
              data.cnsmMUniqueNewErrors.add(tuple);
            }

            // Foreign key check
            else if ((!data.sampleOriginalDigest.pksContains(tuple.getFk())
                && !data.sampleNewDigest.pksContains(tuple.getFk()))
                || (!data.sampleOriginalDigest.pksContains(tuple.getSecondaryFk())
                && !data.sampleNewDigest.pksContains(tuple.getSecondaryFk()))) {
              data.cnsmMRelationErrors.add(tuple);
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
            if (data.cnsmPOriginalDigest.pksContains(tuple.getPk())) {
              data.cnsmPUniqueOriginalErrors.add(tuple);
            }

            // Uniqueness check against new data
            else if (pks.contains(tuple.getPk())) {
              data.cnsmPUniqueNewErrors.add(tuple);
            }

            // Foreign key check
            else if (!data.cnsmMOriginalDigest.pksContains(tuple.getFk())
                && !data.cnsmMNewDigest.pksContains(tuple.getFk())) {
              data.cnsmPRelationErrors.add(tuple);
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
            if (!data.cnsmPOriginalDigest.pksContains(tuple.getFk())
                && !data.cnsmPNewDigest.pksContains(tuple.getFk())) {
              data.cnsmSRelationErrors.add(tuple);
            }

            // Valid data
            else {
              checkState(!tuple.hasPk(), "TODO");
              surjectionEncountered.add(checkNotNull(tuple.getFk()));
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
    bufferedReader.close();

    // Surjectivity
    if (submissionType.isIncrementalData()) {
      if (fileType.hasSimpleSurjectiveRelation()) {
        surjectivity.validateSimpleSurjection(fileType, data, surjectionEncountered);
      }

      if (fileType.hasComplexSurjectiveRelation()) {
        surjectivity.addEncounteredSamples(surjectionEncountered);
      }
    }

    return new FileDigest(submissionType, fileType, path, false, pks, fks, tuples); // TODO: builder
  }

  private Tuple getTuple(FileType fileType, List<String> row) {
    Keys pk = null, fk1 = null, fk2 = null;

    // Clinical
    if (fileType == DONOR) {
      pk = Keys.from(row, DONOR_PKS);
      fk1 = Keys.NOT_APPLICABLE;
      fk2 = Keys.NOT_APPLICABLE;
    } else if (fileType == SPECIMEN) {
      pk = Keys.from(row, SPECIMEN_PKS);
      fk1 = Keys.from(row, SPECIMEN_FKS);
      fk2 = Keys.NOT_APPLICABLE;
    } else if (fileType == SAMPLE) {
      pk = Keys.from(row, SAMPLE_PKS);
      fk1 = Keys.from(row, SAMPLE_FKS);
      fk2 = Keys.NOT_APPLICABLE;
    }

    // Ssm
    else if (fileType == SSM_M) {
      pk = Keys.from(row, SSM_M_PKS);
      fk1 = Keys.from(row, SSM_M_FKS1);
      fk2 = Keys.from(row, SSM_M_FKS2); // TODO: handle case where value is null or a missing code
    } else if (fileType == SSM_P) {
      pk = Keys.NOT_APPLICABLE;
      fk1 = Keys.from(row, SSM_P_FKS);
      fk2 = Keys.NOT_APPLICABLE;
    }

    // Cnsm
    else if (fileType == CNSM_M) {
      pk = Keys.from(row, CNSM_M_PKS);
      fk1 = Keys.from(row, CNSM_M_FKS1);
      fk2 = Keys.from(row, CNSM_M_FKS2); // TODO: handle case where value is null or a missing code
    } else if (fileType == CNSM_P) {
      pk = Keys.from(row, CNSM_P_PKS);
      fk1 = Keys.from(row, CNSM_P_FKS);
      fk2 = Keys.NOT_APPLICABLE;
    } else if (fileType == CNSM_S) {
      pk = Keys.NOT_APPLICABLE;
      fk1 = Keys.from(row, CNSM_S_FKS);
      fk2 = Keys.NOT_APPLICABLE;
    }

    checkState(pk != null || fk1 != null, "TODO: '%s'", row);
    return new Tuple(pk, fk1, fk2);
  }

  private void logProcessedLine(int lineCount, boolean finished) {
    log.info("'{}' lines processed" + (finished ? " (finished)" : ""), lineCount);
  }
}
