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

import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.submission.validation.kv.Helper.TO_BE_REMOVED_FILE_NAME;
import static org.icgc.dcc.submission.validation.kv.Helper.getDataFilePath;
import static org.icgc.dcc.submission.validation.kv.Helper.hasNewClinicalData;
import static org.icgc.dcc.submission.validation.kv.Helper.hasNewCnsmData;
import static org.icgc.dcc.submission.validation.kv.Helper.hasNewSsmData;
import static org.icgc.dcc.submission.validation.kv.Helper.hasOriginalClinicalData;
import static org.icgc.dcc.submission.validation.kv.Helper.hasOriginalCnsmData;
import static org.icgc.dcc.submission.validation.kv.Helper.hasOriginalData;
import static org.icgc.dcc.submission.validation.kv.Helper.hasOriginalSsmData;
import static org.icgc.dcc.submission.validation.kv.Helper.hasToBeRemovedFile;
import static org.icgc.dcc.submission.validation.kv.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.kv.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.kv.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.kv.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.kv.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.kv.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.kv.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.kv.KVFileType.SSM_P;
import static org.icgc.dcc.submission.validation.kv.SubmissionType.NEW_FILE;
import static org.icgc.dcc.submission.validation.kv.SubmissionType.ORIGINAL_FILE;
import static org.icgc.dcc.submission.validation.kv.SubmissionType.TREATED_AS_ORIGINAL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.kv.deletion.Deletion;
import org.icgc.dcc.submission.validation.kv.deletion.DeletionData;

import com.google.common.base.Splitter;

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

  public static final Splitter TAB_SPLITTER = Splitter.on('\t');

  private final long logThreshold;
  private final KeyValidatorData data = new KeyValidatorData();
  private final KeyValidatorErrors errors = new KeyValidatorErrors();
  private final Surjectivity surjectivity = new Surjectivity();
  private final Deletion deletion = new Deletion();

  public void validate() {

    DeletionData deletionData;
    if (hasToBeRemovedFile()) {
      deletionData = deletion.parseToBeDeletedFile();
    } else {
      deletionData = DeletionData.getEmptyInstance();
      log.info("No '{}' file provided", TO_BE_REMOVED_FILE_NAME);
    }
    log.info("{}", deletionData);

    deletionData.validateWellFormedness();

    if (hasOriginalData()) {
      loadOriginalData();
    } else {
      loadPlaceholderData();
    }
    log.info("{}", data.donorOriginalDigest);
    log.info("{}", data.specimenOriginalDigest);
    log.info("{}", data.sampleOriginalDigest);
    log.info("{}", data.ssmMOriginalDigest);
    log.info("{}", data.ssmPOriginalDigest);
    log.info("{}", data.cnsmMOriginalDigest);
    log.info("{}", data.cnsmPOriginalDigest);
    log.info("{}", data.cnsmSOriginalDigest);

    loadNewData(deletionData);
    log.info("{}", data.donorNewDigest);
    log.info("{}", data.specimenNewDigest);
    log.info("{}", data.sampleNewDigest);
    log.info("{}", data.ssmMNewDigest);
    log.info("{}", data.ssmPNewDigest);
    log.info("{}", data.cnsmMNewDigest);
    log.info("{}", data.cnsmPNewDigest);
    log.info("{}", data.cnsmSNewDigest);

    surjectivity.validateComplexSurjection(data, errors);
    // TODO: report errors

    // TODO: should separate materialization?
    // TODO: combine for materialization?
    // Set clinical data (there can only be one set)
    if (hasNewClinicalData()) {
      data.donorDigest = data.donorNewDigest;
      data.specimenDigest = data.specimenNewDigest;
      data.sampleDigest = data.sampleNewDigest;
    } else {
      data.donorDigest = data.donorOriginalDigest;
      data.specimenDigest = data.specimenOriginalDigest;
      data.sampleDigest = data.sampleOriginalDigest;
    }

    boolean valid = errors.describe();
    log.info("{}", valid);

    log.info("done.");
  }

  private void loadOriginalData() {

    // Original clinical
    checkState(hasOriginalClinicalData(), "TODO");
    data.donorOriginalDigest = new FileDigest(
        ORIGINAL_FILE, DONOR, getDataFilePath(ORIGINAL_FILE, DONOR),
        data, errors, surjectivity, logThreshold);

    data.specimenOriginalDigest = new FileDigest(
        ORIGINAL_FILE, SPECIMEN, getDataFilePath(ORIGINAL_FILE, SPECIMEN),
        data, errors, surjectivity, logThreshold);

    data.sampleOriginalDigest = new FileDigest(
        ORIGINAL_FILE, SAMPLE, getDataFilePath(ORIGINAL_FILE, SAMPLE),
        data, errors, surjectivity, logThreshold);

    // Original ssm
    if (hasOriginalSsmData()) {
      data.ssmMOriginalDigest = new FileDigest(
          ORIGINAL_FILE, SSM_M, getDataFilePath(ORIGINAL_FILE, SSM_M),
          data, errors, surjectivity, logThreshold);
      // // TODO: May not have to load p?
      data.ssmPOriginalDigest = new FileDigest(
          ORIGINAL_FILE, SSM_P, getDataFilePath(ORIGINAL_FILE, SSM_P),
          data, errors, surjectivity, logThreshold);
    } else {
      data.ssmMOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, SSM_M);
      data.ssmPOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, SSM_P);
    }

    // Original cnsm
    if (hasOriginalCnsmData()) {
      data.cnsmMOriginalDigest = new FileDigest(
          ORIGINAL_FILE, CNSM_M, getDataFilePath(ORIGINAL_FILE, CNSM_M),
          data, errors, surjectivity, logThreshold);
      data.cnsmPOriginalDigest = new FileDigest(
          ORIGINAL_FILE, CNSM_P, getDataFilePath(ORIGINAL_FILE, CNSM_P),
          data, errors, surjectivity, logThreshold);
      // TODO: May not have to load s?
      data.cnsmSOriginalDigest = new FileDigest(
          ORIGINAL_FILE, CNSM_S, getDataFilePath(ORIGINAL_FILE, CNSM_S),
          data, errors, surjectivity, logThreshold);
    } else {
      data.cnsmMOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, CNSM_M);
      data.cnsmPOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, CNSM_P);
      data.cnsmSOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, CNSM_S);
    }
  }

  private void loadPlaceholderData() {
    data.donorOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, DONOR);
    data.specimenOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, SPECIMEN);
    data.sampleOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, SAMPLE);

    data.ssmMOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, SSM_M);
    data.ssmPOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, SSM_P);

    data.cnsmMOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, CNSM_M);
    data.cnsmPOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, CNSM_P);
    data.cnsmSOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, CNSM_S);
  }

  /**
   * Order matters!
   */
  private void loadNewData(DeletionData deletionData) {

    boolean valid = deletionData.validateAgainstOldClinicalData(data);
    if (!valid) {
      System.exit(1); // FIXME
    } else {
      log.info("ok1");
    }

    // New clinical
    if (hasNewClinicalData()) {
      data.donorNewDigest = new FileDigest(
          TREATED_AS_ORIGINAL, DONOR, getDataFilePath(NEW_FILE, DONOR),
          data, errors, surjectivity, logThreshold);

      data.specimenNewDigest = new FileDigest(
          TREATED_AS_ORIGINAL, SPECIMEN, getDataFilePath(NEW_FILE, SPECIMEN),
          data, errors, surjectivity, logThreshold);

      data.sampleNewDigest = new FileDigest(
          TREATED_AS_ORIGINAL, SAMPLE, getDataFilePath(NEW_FILE, SAMPLE),
          data, errors, surjectivity, logThreshold);

      valid = deletionData.validateAgainstNewClinicalData(data);
      if (!valid) {
        System.exit(1); // FIXME
      } else {
        log.info("ok2");
      }
    }

    // New ssm
    if (hasNewSsmData()) {
      data.ssmMNewDigest = new FileDigest(
          NEW_FILE, SSM_M, getDataFilePath(NEW_FILE, SSM_M),
          data, errors, surjectivity, logThreshold);
      data.ssmPNewDigest = new FileDigest(
          NEW_FILE, SSM_P, getDataFilePath(NEW_FILE, SSM_P),
          data, errors, surjectivity, logThreshold);
    }

    // New cnsm
    if (hasNewCnsmData()) {
      data.cnsmMNewDigest = new FileDigest(
          NEW_FILE, CNSM_M, getDataFilePath(NEW_FILE, CNSM_M),
          data, errors, surjectivity, logThreshold);
      data.cnsmPNewDigest = new FileDigest(
          NEW_FILE, CNSM_P, getDataFilePath(NEW_FILE, CNSM_P),
          data, errors, surjectivity, logThreshold);
      data.cnsmSNewDigest = new FileDigest(
          NEW_FILE, CNSM_S, getDataFilePath(NEW_FILE, CNSM_S),
          data, errors, surjectivity, logThreshold);
    }
  }
}
