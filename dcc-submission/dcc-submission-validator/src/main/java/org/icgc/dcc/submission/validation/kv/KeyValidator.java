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
import static org.icgc.dcc.submission.validation.kv.SubmissionType.NEW_FILE;
import static org.icgc.dcc.submission.validation.kv.SubmissionType.ORIGINAL_FILE;
import static org.icgc.dcc.submission.validation.kv.SubmissionType.TREATED_AS_ORIGINAL;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    // TODO: combine for materialization
    log.info("done.");
  }

  private void loadOriginalData() {

    // Original clinical
    checkState(hasOriginalClinical(), "TODO");
    data.donorOriginalDigest = new FileDigest(
        ORIGINAL_FILE, DONOR, getPath(ORIGINAL_FILE, DONOR),
        data, surjectivity, logThreshold);
    log.info("{}", data.donorOriginalDigest);

    data.specimenOriginalDigest = new FileDigest(
        ORIGINAL_FILE, SPECIMEN, getPath(ORIGINAL_FILE, SPECIMEN),
        data, surjectivity, logThreshold);
    log.info("{}", data.specimenOriginalDigest);

    data.sampleOriginalDigest = new FileDigest(
        ORIGINAL_FILE, SAMPLE, getPath(ORIGINAL_FILE, SAMPLE),
        data, surjectivity, logThreshold);
    log.info("{}", data.sampleOriginalDigest);

    // Original ssm
    if (hasOriginalSsm()) {
      data.ssmMOriginalDigest = new FileDigest(
          ORIGINAL_FILE, SSM_M, getPath(ORIGINAL_FILE, SSM_M),
          data, surjectivity, logThreshold);
      // // TODO: May not have to load p?
      data.ssmPOriginalDigest = new FileDigest(
          ORIGINAL_FILE, SSM_P, getPath(ORIGINAL_FILE, SSM_P),
          data, surjectivity, logThreshold);
    } else {
      data.ssmMOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, SSM_M);
      data.ssmPOriginalDigest = FileDigest.getEmptyInstance(ORIGINAL_FILE, SSM_P);
    }
    log.info("{}", data.ssmMOriginalDigest);
    log.info("{}", data.ssmPOriginalDigest);

    // Original cnsm
    if (hasOriginalCnsm()) {
      data.cnsmMOriginalDigest = new FileDigest(
          ORIGINAL_FILE, CNSM_M, getPath(ORIGINAL_FILE, CNSM_M),
          data, surjectivity, logThreshold);
      data.cnsmPOriginalDigest = new FileDigest(
          ORIGINAL_FILE, CNSM_P, getPath(ORIGINAL_FILE, CNSM_P),
          data, surjectivity, logThreshold);
      // TODO: May not have to load s?
      data.cnsmSOriginalDigest = new FileDigest(
          ORIGINAL_FILE, CNSM_S, getPath(ORIGINAL_FILE, CNSM_S),
          data, surjectivity, logThreshold);
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
      data.donorNewDigest = new FileDigest(
          TREATED_AS_ORIGINAL, DONOR, getPath(NEW_FILE, DONOR),
          data, surjectivity, logThreshold);
      log.info("{}", data.donorNewDigest);

      data.specimenNewDigest = new FileDigest(
          TREATED_AS_ORIGINAL, SPECIMEN, getPath(NEW_FILE, SPECIMEN),
          data, surjectivity, logThreshold);
      log.info("{}", data.specimenNewDigest);

      data.sampleNewDigest = new FileDigest(
          TREATED_AS_ORIGINAL, SAMPLE, getPath(NEW_FILE, SAMPLE),
          data, surjectivity, logThreshold);
      log.info("{}", data.sampleNewDigest);
    }

    // New ssm
    if (hasNewSsm()) {
      data.ssmMNewDigest = new FileDigest(
          NEW_FILE, SSM_M, getPath(NEW_FILE, SSM_M),
          data, surjectivity, logThreshold);
      data.ssmPNewDigest = new FileDigest(
          NEW_FILE, SSM_P, getPath(NEW_FILE, SSM_P),
          data, surjectivity, logThreshold);
    }
    log.info("{}", data.ssmMNewDigest);
    log.info("{}", data.ssmPNewDigest);

    // New cnsm
    if (hasNewCnsm()) {
      data.cnsmMNewDigest = new FileDigest(
          NEW_FILE, CNSM_M, getPath(NEW_FILE, CNSM_M),
          data, surjectivity, logThreshold);
      data.cnsmPNewDigest = new FileDigest(
          NEW_FILE, CNSM_P, getPath(NEW_FILE, CNSM_P),
          data, surjectivity, logThreshold);
      data.cnsmSNewDigest = new FileDigest(
          NEW_FILE, CNSM_S, getPath(NEW_FILE, CNSM_S),
          data, surjectivity, logThreshold);
    }
    log.info("{}", data.cnsmMNewDigest);
    log.info("{}", data.cnsmPNewDigest);
    log.info("{}", data.cnsmSNewDigest);
  }
}
