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
package org.icgc.dcc.submission.validation.key;

import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.submission.validation.key.KVConstants.RELATIONS;
import static org.icgc.dcc.submission.validation.key.KVUtils.getDataFilePath;
import static org.icgc.dcc.submission.validation.key.KVUtils.hasExistingClinicalData;
import static org.icgc.dcc.submission.validation.key.KVUtils.hasExistingCnsmData;
import static org.icgc.dcc.submission.validation.key.KVUtils.hasExistingData;
import static org.icgc.dcc.submission.validation.key.KVUtils.hasExistingSsmData;
import static org.icgc.dcc.submission.validation.key.KVUtils.hasIncrementalClinicalData;
import static org.icgc.dcc.submission.validation.key.KVUtils.hasIncrementalCnsmData;
import static org.icgc.dcc.submission.validation.key.KVUtils.hasIncrementalSsmData;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType.EXISTING_FILE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType.INCREMENTAL_FILE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType.TREATED_AS_ORIGINAL;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.cascading.CascadeExecutor;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.key.data.KVExistingFileDataDigest;
import org.icgc.dcc.submission.validation.key.data.KVFileDataDigest;
import org.icgc.dcc.submission.validation.key.data.KVIncrementalFileDataDigest;
import org.icgc.dcc.submission.validation.key.data.KVSubmissionDataDigest;
import org.icgc.dcc.submission.validation.key.deletion.DeletionData;
import org.icgc.dcc.submission.validation.key.deletion.DeletionFileParser;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType;
import org.icgc.dcc.submission.validation.key.error.KVSubmissionErrors;
import org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

@Slf4j
@RequiredArgsConstructor
public class KeyValidator implements Validator, Runnable {

  public static final String COMPONENT_NAME = "Key Validator";

  private final long logThreshold;
  private final SurjectivityValidator surjectivityValidator = new SurjectivityValidator();
  private final KVSubmissionDataDigest existingData = new KVSubmissionDataDigest();
  private final KVSubmissionDataDigest incrementalData = new KVSubmissionDataDigest();
  private final KVSubmissionErrors errors = new KVSubmissionErrors();

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public void validate(ValidationContext context) throws InterruptedException {
    new CascadeExecutor(context.getPlatformStrategy()).execute(this);
  }

  @Override
  public void run() {
    validate();
  }

  public void validate() {
    // Validate deletion data
    val deletionData = DeletionData.getInstance();
    // validateDeletions(deletionData);

    // Process existing data
    if (hasExistingData()) {
      loadExistingData();
    } else {
      loadPlaceholderExistingFiles();
    }
    for (val entry : existingData.entrySet()) {
      log.info("{}: {}", entry.getKey(), entry.getValue());
    }

    // Process incremental data
    loadIncrementalData(deletionData);
    for (val entry : incrementalData.entrySet()) {
      log.info("{}: {}", entry.getKey(), entry.getValue());
    }

    // Surjection validation (can only be done at the very end)
    validateComplexSurjection();

    // Report
    boolean valid = errors.describe(); // TODO: prettify
    log.info("{}", valid);
    log.info("done.");
  }

  public void validateDeletions(DeletionData deletionData) {
    boolean valid;
    valid = deletionData.validateWellFormedness();
    if (!valid) {
      log.error("Deletion well-formedness errors found");
    }

    val existingDonorIds = hasExistingClinicalData() ?
        DeletionFileParser.getExistingDonorIds() :
        Sets.<String> newTreeSet();
    valid = deletionData.validateAgainstOldClinicalData(existingDonorIds);
    if (!valid) {
      log.error("Deletion previous data errors found");
    }

    if (hasIncrementalClinicalData()) {
      valid = deletionData.validateAgainstIncrementalClinicalData(
          existingDonorIds, DeletionFileParser.getIncrementalDonorIds());
      if (!valid) {
        log.error("Deletion incremental data errors found");
      }
    }
  }

  private void loadExistingData() {
    log.info("Loading existing data");

    // Existing clinical
    checkState(hasExistingClinicalData(), "TODO"); // At this point we expect it
    loadExistingFile(DONOR);
    loadExistingFile(SPECIMEN);
    loadExistingFile(SAMPLE);

    // Existing ssm
    if (hasExistingSsmData()) {
      loadExistingFile(SSM_M);
      loadExistingFile(SSM_P);
    } else {
      loadPlaceholderExistingFile(SSM_M);
      loadPlaceholderExistingFile(SSM_P);
    }

    // Existing cnsm
    if (hasExistingCnsmData()) {
      loadExistingFile(CNSM_M);
      loadExistingFile(CNSM_P);
      loadExistingFile(CNSM_S);
    } else {
      loadPlaceholderExistingFile(CNSM_M);
      loadPlaceholderExistingFile(CNSM_P);
      loadPlaceholderExistingFile(CNSM_S);
    }
  }

  /**
   * Order matters!
   */
  private void loadIncrementalData(DeletionData deletionData) {
    log.info("Loading incremental data");

    // Incremental clinical
    if (hasIncrementalClinicalData()) {
      loadIncrementalFile(DONOR, TREATED_AS_ORIGINAL, deletionData);
      loadIncrementalFile(SPECIMEN, TREATED_AS_ORIGINAL, deletionData);
      loadIncrementalFile(SAMPLE, TREATED_AS_ORIGINAL, deletionData);
    }

    // Incremental ssm
    if (hasIncrementalSsmData()) {
      loadIncrementalFile(SSM_M, INCREMENTAL_FILE, deletionData);
      loadIncrementalFile(SSM_P, INCREMENTAL_FILE, deletionData);
    }

    // Incremental cnsm
    if (hasIncrementalCnsmData()) {
      loadIncrementalFile(CNSM_M, INCREMENTAL_FILE, deletionData);
      loadIncrementalFile(CNSM_P, INCREMENTAL_FILE, deletionData);
      loadIncrementalFile(CNSM_S, INCREMENTAL_FILE, deletionData);
    }
  }

  private void loadExistingFile(KVFileType fileType) {
    log.info("Loading existing file: '{}'", fileType);
    existingData.put(
        fileType,
        new KVExistingFileDataDigest(
            new KVFileDescription(
                EXISTING_FILE,
                fileType,
                Optional.<String> of(getDataFilePath(EXISTING_FILE, fileType))),
            logThreshold)
            .processFile());
  }

  private void loadIncrementalFile(KVFileType fileType, KVSubmissionType submissionType, DeletionData deletionData) {
    log.info("Loading incremental file: '{}.{}'", fileType, submissionType);
    incrementalData.put(
        fileType,
        new KVIncrementalFileDataDigest( // TODO: address ugliness
            new KVFileDescription(
                submissionType,
                fileType,
                Optional.<String> of(getDataFilePath(INCREMENTAL_FILE, fileType))),
            logThreshold,
            deletionData,

            existingData.get(fileType),
            existingData.get(RELATIONS.get(fileType)),
            incrementalData.get(RELATIONS.get(fileType)),

            errors.getFileErrors(fileType),
            errors.getFileErrors(RELATIONS.get(fileType)), // May be null (for DONOR for instance)

            surjectivityValidator)
            .processFile());
  }

  private void loadPlaceholderExistingFiles() {
    log.info("Loading placeholder existing files");
    loadPlaceholderExistingFile(DONOR);
    loadPlaceholderExistingFile(SPECIMEN);
    loadPlaceholderExistingFile(SAMPLE);

    loadPlaceholderExistingFile(SSM_M);
    loadPlaceholderExistingFile(SSM_P);

    loadPlaceholderExistingFile(CNSM_M);
    loadPlaceholderExistingFile(CNSM_P);
    loadPlaceholderExistingFile(CNSM_S);
  }

  private void loadPlaceholderExistingFile(KVFileType fileType) {
    log.info("Loading placeholder existing file: '{}'", fileType);
    existingData.put(
        fileType,
        KVFileDataDigest.getEmptyInstance(
            new KVFileDescription(
                EXISTING_FILE,
                fileType,
                Optional.<String> absent())
            ));
  }

  private void validateComplexSurjection() {
    log.info("Validating complex surjection");
    surjectivityValidator.validateComplexSurjection(
        existingData.get(SAMPLE),
        incrementalData.get(SAMPLE),
        errors.getFileErrors(SAMPLE));
  }

}
