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
import static org.icgc.dcc.submission.validation.kv.Helper.getDataFilePath;
import static org.icgc.dcc.submission.validation.kv.Helper.hasNewClinicalData;
import static org.icgc.dcc.submission.validation.kv.Helper.hasNewCnsmData;
import static org.icgc.dcc.submission.validation.kv.Helper.hasNewSsmData;
import static org.icgc.dcc.submission.validation.kv.Helper.hasOriginalClinicalData;
import static org.icgc.dcc.submission.validation.kv.Helper.hasOriginalCnsmData;
import static org.icgc.dcc.submission.validation.kv.Helper.hasOriginalData;
import static org.icgc.dcc.submission.validation.kv.Helper.hasOriginalSsmData;
import static org.icgc.dcc.submission.validation.kv.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.kv.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.kv.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.kv.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.kv.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.kv.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.kv.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.kv.KVFileType.SSM_P;
import static org.icgc.dcc.submission.validation.kv.KVSubmissionErrors.RELATIONS;
import static org.icgc.dcc.submission.validation.kv.KVSubmissionType.NEW_FILE;
import static org.icgc.dcc.submission.validation.kv.KVSubmissionType.ORIGINAL_FILE;
import static org.icgc.dcc.submission.validation.kv.KVSubmissionType.TREATED_AS_ORIGINAL;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.kv.deletion.DeletionData;
import org.icgc.dcc.submission.validation.kv.deletion.DeletionFileParser;
import org.icgc.dcc.submission.validation.kv.error.KVFileErrors;

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
  private final DeletionFileParser deletionParser = new DeletionFileParser();
  private final SurjectivityValidator surjectivityValidator = new SurjectivityValidator();
  private final KVSubmissionDataDigest originalData = new KVSubmissionDataDigest();
  private final KVSubmissionDataDigest newData = new KVSubmissionDataDigest();
  private final KVSubmissionErrors errors = new KVSubmissionErrors();

  public void validate() {

    // Deletion data
    val deletionData = DeletionData.validate(deletionParser);

    // Old data
    if (hasOriginalData()) {
      loadOriginalData(deletionData);
    } else {
      loadPlaceholderData();
    }
    for (val entry : originalData.entrySet()) {
      log.info("{}: {}", entry.getKey(), entry.getValue());
    }

    // New data
    loadNewData(deletionData);
    for (val entry : newData.entrySet()) {
      log.info("{}: {}", entry.getKey(), entry.getValue());
    }

    // Surjection validation
    validateComplexSurjection();

    // Report
    boolean valid = errors.describe(); // TODO: prettify
    log.info("{}", valid);
    log.info("done.");
  }

  private void loadOriginalData(DeletionData deletionData) { // TODO: deletionData not really needed here

    // Original clinical
    checkState(hasOriginalClinicalData(), "TODO");
    loadOriginalFile(DONOR);
    loadOriginalFile(SPECIMEN);
    loadOriginalFile(SAMPLE);

    // Original ssm
    if (hasOriginalSsmData()) {
      loadOriginalFile(SSM_M);
      loadOriginalFile(SSM_P);
    } else {
      loadEmptyOriginalFile(SSM_M);
      loadEmptyOriginalFile(SSM_P);
    }

    // Original cnsm
    if (hasOriginalCnsmData()) {
      loadOriginalFile(CNSM_M);
      loadOriginalFile(CNSM_P);
      loadOriginalFile(CNSM_S);
    } else {
      loadEmptyOriginalFile(CNSM_M);
      loadEmptyOriginalFile(CNSM_P);
      loadEmptyOriginalFile(CNSM_S);
    }
  }

  private void loadPlaceholderData() {
    originalData.put(DONOR, KVFileDataDigest.getEmptyInstance(ORIGINAL_FILE, DONOR));
    originalData.put(SPECIMEN, KVFileDataDigest.getEmptyInstance(ORIGINAL_FILE, SPECIMEN));
    originalData.put(SAMPLE, KVFileDataDigest.getEmptyInstance(ORIGINAL_FILE, SAMPLE));

    originalData.put(SSM_M, KVFileDataDigest.getEmptyInstance(ORIGINAL_FILE, SSM_M));
    originalData.put(SSM_P, KVFileDataDigest.getEmptyInstance(ORIGINAL_FILE, SSM_P));

    originalData.put(CNSM_M, KVFileDataDigest.getEmptyInstance(ORIGINAL_FILE, CNSM_M));
    originalData.put(CNSM_P, KVFileDataDigest.getEmptyInstance(ORIGINAL_FILE, CNSM_P));
    originalData.put(CNSM_S, KVFileDataDigest.getEmptyInstance(ORIGINAL_FILE, CNSM_S));
  }

  /**
   * Order matters!
   */
  private void loadNewData(DeletionData deletionData) {

    // New clinical
    if (hasNewClinicalData()) {
      loadNewFile(DONOR, TREATED_AS_ORIGINAL, deletionData);
      loadNewFile(SPECIMEN, TREATED_AS_ORIGINAL, deletionData);
      loadNewFile(SAMPLE, TREATED_AS_ORIGINAL, deletionData);
    }

    // New ssm
    if (hasNewSsmData()) {
      loadNewFile(SSM_M, NEW_FILE, deletionData);
      loadNewFile(SSM_P, NEW_FILE, deletionData);
    }

    // New cnsm
    if (hasNewCnsmData()) {
      loadNewFile(CNSM_M, NEW_FILE, deletionData);
      loadNewFile(CNSM_P, NEW_FILE, deletionData);
      loadNewFile(CNSM_S, NEW_FILE, deletionData);
    }
  }

  private void loadOriginalFile(KVFileType fileType) {
    originalData.put(
        fileType,
        new KVFileDataDigest(
            ORIGINAL_FILE, fileType, getDataFilePath(ORIGINAL_FILE, fileType),
            (DeletionData) null,
            (KVFileDataDigest) null, (KVFileDataDigest) null, (KVFileDataDigest) null, // TODO: ugly, need strategy
            (KVFileErrors) null, (KVFileErrors) null,
            surjectivityValidator, logThreshold));
  }

  private void loadNewFile(KVFileType fileType, KVSubmissionType submissionType, DeletionData deletionData) {
    newData.put(
        fileType,
        new KVFileDataDigest( // TODO: address ugliness
            submissionType, fileType, getDataFilePath(NEW_FILE, fileType),
            deletionData,

            originalData.get(fileType),
            originalData.get(RELATIONS.get(fileType)),
            newData.get(RELATIONS.get(fileType)),

            errors.getFileErrors(fileType),
            errors.getFileErrors(RELATIONS.get(fileType)), // May be null (for DONOR for instance)

            surjectivityValidator, logThreshold));
  }

  private void loadEmptyOriginalFile(KVFileType fileType) {
    originalData.put(
        fileType,
        KVFileDataDigest.getEmptyInstance(ORIGINAL_FILE, fileType));
  }

  private void validateComplexSurjection() {
    surjectivityValidator.validateComplexSurjection(
        originalData.get(SAMPLE),
        newData.get(SAMPLE),
        errors.getFileErrors(SAMPLE));
  }
}
