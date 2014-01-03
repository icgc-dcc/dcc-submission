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
package org.icgc.dcc.submission.validation.key.core;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.icgc.dcc.submission.validation.core.ErrorType.REVERSE_RELATION_FILE_ERROR;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.RELATIONS;
import static org.icgc.dcc.submission.validation.key.core.KVFileDescription.getExistingFileDescription;
import static org.icgc.dcc.submission.validation.key.core.KVFileDescription.getIncrementalFileDescription;
import static org.icgc.dcc.submission.validation.key.core.KVFileDescription.getPlaceholderFileDescription;
import static org.icgc.dcc.submission.validation.key.core.KVUtils.getDataFilePath;
import static org.icgc.dcc.submission.validation.key.core.KVUtils.hasExistingClinicalData;
import static org.icgc.dcc.submission.validation.key.core.KVUtils.hasExistingCnsmData;
import static org.icgc.dcc.submission.validation.key.core.KVUtils.hasExistingData;
import static org.icgc.dcc.submission.validation.key.core.KVUtils.hasExistingSsmData;
import static org.icgc.dcc.submission.validation.key.core.KVUtils.hasIncrementalClinicalData;
import static org.icgc.dcc.submission.validation.key.core.KVUtils.hasIncrementalCnsmData;
import static org.icgc.dcc.submission.validation.key.core.KVUtils.hasIncrementalSsmData;
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
import static org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType.INCREMENTAL_TO_BE_TREATED_AS_EXISTING;
import static org.icgc.dcc.submission.validation.key.error.KVError.kvError;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.data.KVExistingFileDataDigest;
import org.icgc.dcc.submission.validation.key.data.KVFileDataDigest;
import org.icgc.dcc.submission.validation.key.data.KVIncrementalFileDataDigest;
import org.icgc.dcc.submission.validation.key.data.KVSubmissionDataDigest;
import org.icgc.dcc.submission.validation.key.deletion.DeletionData;
import org.icgc.dcc.submission.validation.key.deletion.DeletionFileParser;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType;
import org.icgc.dcc.submission.validation.key.error.KVSubmissionErrors;
import org.icgc.dcc.submission.validation.key.report.KVReport;
import org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

@Slf4j
@RequiredArgsConstructor
public class KVValidator {

  private final KVReport report;
  private final long logThreshold;

  private final SurjectivityValidator surjectivityValidator = new SurjectivityValidator();
  private final KVSubmissionDataDigest existingData = new KVSubmissionDataDigest();
  private final KVSubmissionDataDigest incrementalData = new KVSubmissionDataDigest();
  private final KVSubmissionErrors errors = new KVSubmissionErrors();

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
    log.info("{}", repeat("=", 75));
    for (val entry : existingData.entrySet()) {
      log.info("{}: {}", entry.getKey(), entry.getValue());
    }
    log.info("{}", repeat("=", 75));

    // Process incremental data
    loadIncrementalData(deletionData);
    log.info("{}", repeat("=", 75));
    for (val entry : incrementalData.entrySet()) {
      log.info("{}: {}", entry.getKey(), entry.getValue());
    }
    log.info("{}", repeat("=", 75));

    // Surjection validation (can only be done at the very end)
    validateComplexSurjection();

    // Report
    boolean valid = errors.describe(incrementalData.getFileDescriptions()); // TODO: prettify
    log.info("{}", valid);
    log.info("done.");

    // TODO: Remove. This is just to show an example of how to report an error
    report.report(
        kvError()
            .fileName("ssm_p.txt")
            .type(REVERSE_RELATION_FILE_ERROR)
            .build());
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
    log.info("Processing exiting clinical data");
    loadExistingFile(DONOR);
    loadExistingFile(SPECIMEN);
    loadExistingFile(SAMPLE);

    // Existing ssm
    if (hasExistingSsmData()) {
      log.info("Processing exiting ssm data");
      loadExistingFile(SSM_M);
      loadExistingFile(SSM_P);
    } else {
      log.info("No exiting ssm data");
      loadPlaceholderExistingFile(SSM_M);
      loadPlaceholderExistingFile(SSM_P);
    }

    // Existing cnsm
    if (hasExistingCnsmData()) {
      log.info("Processing exiting cnsm data");
      loadExistingFile(CNSM_M);
      loadExistingFile(CNSM_P);
      loadExistingFile(CNSM_S);
    } else {
      log.info("No exiting cnsm data");
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
      log.info("Processing incremental clinical data");
      loadIncrementalFile(DONOR, INCREMENTAL_TO_BE_TREATED_AS_EXISTING, deletionData);
      loadIncrementalFile(SPECIMEN, INCREMENTAL_TO_BE_TREATED_AS_EXISTING, deletionData);
      loadIncrementalFile(SAMPLE, INCREMENTAL_TO_BE_TREATED_AS_EXISTING, deletionData);
    } else {
      log.info("No incremental clinical data");
    }

    // Incremental ssm
    if (hasIncrementalSsmData()) {
      log.info("Processing incremental ssm data");
      loadIncrementalFile(SSM_M, INCREMENTAL_FILE, deletionData);
      loadIncrementalFile(SSM_P, INCREMENTAL_FILE, deletionData);
    } else {
      log.info("No incremental ssm data");
    }

    // Incremental cnsm
    if (hasIncrementalCnsmData()) {
      log.info("Processing incremental cnsm data");
      loadIncrementalFile(CNSM_M, INCREMENTAL_FILE, deletionData);
      loadIncrementalFile(CNSM_P, INCREMENTAL_FILE, deletionData);
      loadIncrementalFile(CNSM_S, INCREMENTAL_FILE, deletionData);
    } else {
      log.info("No incremental cnsm data");
    }
  }

  private void loadExistingFile(KVFileType fileType) {
    log.info("{}", repeat("=", 75));
    log.info("Loading existing file: '{}'", fileType);
    val dataFilePath = getDataFilePath(EXISTING_FILE, fileType);
    existingData.put(
        fileType,
        new KVExistingFileDataDigest(
            getExistingFileDescription(fileType, dataFilePath),
            logThreshold)
            .processFile());
  }

  private void loadIncrementalFile(KVFileType fileType, KVSubmissionType submissionType, DeletionData deletionData) {
    log.info("{}", repeat("=", 75));
    log.info("Loading incremental file: '{}.{}'", fileType, submissionType);
    val dataFilePath = getDataFilePath(INCREMENTAL_FILE, fileType);

    incrementalData.put(
        fileType,
        new KVIncrementalFileDataDigest(
            getIncrementalFileDescription(
                submissionType.isIncrementalToBeTreatedAsExisting(), fileType, dataFilePath),
            logThreshold,
            deletionData,

            existingData.get(fileType),
            existingData.get(RELATIONS.get(fileType)),
            getOptionalReferencedData(fileType),

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
        KVFileDataDigest.getEmptyInstance(getPlaceholderFileDescription(fileType)));
  }

  private Optional<KVFileDataDigest> getOptionalReferencedData(KVFileType fileType) {
    val referencedFileType = RELATIONS.get(fileType);
    if (referencedFileType == null) {
      log.info("No referenced file type for '{}'", DONOR);
      checkState(fileType == DONOR, "TODO");
      return absent();
    } else {
      if (incrementalData.contains(referencedFileType)) { // May not if not re-submitted
        log.info("Incremental data contains '{}'", referencedFileType);
        return of(incrementalData.get(referencedFileType));
      } else { // Fall back on existing data
        log.info("Incremental data does not contain '{}', falling back on existing data", referencedFileType);
        return absent();
      }
    }
  }

  private void validateComplexSurjection() {
    log.info("{}", repeat("=", 75));
    log.info("Validating complex surjection");
    surjectivityValidator.validateComplexSurjection(
        existingData.get(SAMPLE),
        incrementalData.get(SAMPLE),
        errors.getFileErrors(SAMPLE));
    log.info("{}", repeat("=", 75));
  }

}
