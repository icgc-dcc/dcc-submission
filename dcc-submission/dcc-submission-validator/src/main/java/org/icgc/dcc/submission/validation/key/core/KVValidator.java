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
import static org.icgc.dcc.submission.validation.key.core.KVConstants.RELATIONS;
import static org.icgc.dcc.submission.validation.key.core.KVFileDescription.getExistingFileDescription;
import static org.icgc.dcc.submission.validation.key.core.KVFileDescription.getIncrementalFileDescription;
import static org.icgc.dcc.submission.validation.key.core.KVFileDescription.getPlaceholderFileDescription;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType.EXISTING_FILE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType.INCREMENTAL_FILE;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.data.KVExistingFileDataDigest;
import org.icgc.dcc.submission.validation.key.data.KVFileDataDigest;
import org.icgc.dcc.submission.validation.key.data.KVIncrementalFileDataDigest;
import org.icgc.dcc.submission.validation.key.data.KVSubmissionDataDigest;
import org.icgc.dcc.submission.validation.key.deletion.DeletionData;
import org.icgc.dcc.submission.validation.key.deletion.DeletionFileParser;
import org.icgc.dcc.submission.validation.key.enumeration.KVExperimentalDataType;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType;
import org.icgc.dcc.submission.validation.key.error.KVFileErrors;
import org.icgc.dcc.submission.validation.key.error.KVSubmissionErrors;
import org.icgc.dcc.submission.validation.key.report.KVReport;
import org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

@Slf4j
public class KVValidator {

  public static final boolean SUPPORT_EXISTING = false;

  @NonNull
  private final KVFileParser kvFileParser;
  @NonNull
  private final KVFileSystem kvFileSystem;
  @NonNull
  private final KVReport kvReport;
  @NonNull
  private final SurjectivityValidator surjectivityValidator;

  private final KVSubmissionDataDigest existingData = new KVSubmissionDataDigest();
  private final KVSubmissionDataDigest incrementalData = new KVSubmissionDataDigest();
  private final KVSubmissionErrors errors = new KVSubmissionErrors();

  public KVValidator(KVFileParser kvFileParser, KVFileSystem kvFileSystem, KVReport kvReport) {
    this.kvFileParser = kvFileParser;
    this.kvFileSystem = kvFileSystem;
    this.kvReport = kvReport;
    this.surjectivityValidator = new SurjectivityValidator(kvFileSystem);
  }

  public void validate() {
    // Validate deletion data
    val deletionData = DeletionData.getInstance(kvFileSystem);
    // validateDeletions(deletionData);

    if (SUPPORT_EXISTING) {
      // Process existing data
      if (kvFileSystem.hasExistingData()) {
        loadExistingData();
      } else {
        loadPlaceholderExistingFiles();
      }
      log.debug("{}", repeat("=", 75));
      for (val entry : existingData.entrySet()) {
        log.debug("{}: {}", entry.getKey(), entry.getValue());
      }
      log.debug("{}", repeat("=", 75));
    }

    // Process incremental data
    validateIncrementalData(deletionData);
    log.info("{}", repeat("=", 75));
    for (val entry : incrementalData.entrySet()) {
      log.debug("{}: {}", entry.getKey(), entry.getValue());
    }
    log.debug("{}", repeat("=", 75));

    // Surjection validation (can only be done at the very end)
    // validateComplexSurjection();

    // Report
    boolean valid = errors.describe(kvReport, incrementalData.getFileDescriptions()); // TODO: prettify
    log.info("{}", valid);
    log.info("done.");
  }

  public void validateDeletions(DeletionData deletionData) {
    boolean valid;
    valid = deletionData.validateWellFormedness();
    if (!valid) {
      log.error("Deletion well-formedness errors found");
    }

    val existingDonorIds = kvFileSystem.hasExistingClinicalData() ?
        DeletionFileParser.getExistingDonorIds(kvFileSystem) :
        Sets.<String> newTreeSet();
    valid = deletionData.validateAgainstOldClinicalData(existingDonorIds);
    if (!valid) {
      log.error("Deletion previous data errors found");
    }

    if (kvFileSystem.hasIncrementalClinicalData()) {
      valid = deletionData.validateAgainstIncrementalClinicalData(
          existingDonorIds, DeletionFileParser.getIncrementalDonorIds(kvFileSystem));
      if (!valid) {
        log.error("Deletion incremental data errors found");
      }
    }
  }

  private void loadExistingData() {
    log.info("Loading existing data");

    // Existing clinical
    checkState(kvFileSystem.hasExistingClinicalData(), "TODO"); // At this point we expect it
    log.info("Processing exiting clinical data");
    loadExistingFile(DONOR);
    loadExistingFile(SPECIMEN);
    loadExistingFile(SAMPLE);

    for (val dataType : KVExperimentalDataType.values()) {

      // Existing data
      if (kvFileSystem.hasExistingData(dataType)) {
        log.info("Processing exiting '{}' data", dataType);
        for (val fileType : dataType.getFileTypes()) {
          loadExistingFile(fileType);
        }
      }

      // No data
      else {
        log.info("No existing '{}' data", dataType);
        for (val fileType : dataType.getFileTypes()) {
          loadPlaceholderExistingFile(fileType);
        }
      }
    }
  }

  /**
   * Order matters!
   */
  private void validateIncrementalData(DeletionData deletionData) {
    log.info("Loading incremental data");

    // Incremental clinical data
    if (kvFileSystem.hasIncrementalClinicalData()) {
      log.info("Processing incremental clinical data");
      loadIncrementalFile(DONOR, INCREMENTAL_FILE, deletionData);
      loadIncrementalFile(SPECIMEN, INCREMENTAL_FILE, deletionData);
      loadIncrementalFile(SAMPLE, INCREMENTAL_FILE, deletionData);
    } else {
      checkState(false, "NOT VALID ANYMORE"); // FIXME
      log.info("No incremental clinical data");
    }

    // Incremental experimental data
    for (val dataType : KVExperimentalDataType.values()) {
      if (kvFileSystem.hasIncrementalData(dataType)) {
        log.info("Processing incremental '{}' data", dataType);
        for (val fileType : dataType.getFileTypes()) {
          loadIncrementalFile(fileType, INCREMENTAL_FILE, deletionData);
        }
      } else {
        log.info("No incremental '{}' data", dataType);
      }
    }
  }

  private void loadExistingFile(KVFileType fileType) {
    log.info("{}", repeat("=", 75));
    log.info("Loading existing file: '{}'", fileType);
    val dataFilePath = kvFileSystem.getDataFilePath(EXISTING_FILE, fileType);
    existingData.put(
        fileType,
        new KVExistingFileDataDigest(
            kvFileParser,
            getExistingFileDescription(fileType, dataFilePath),
            1000000)
            .processFile());
  }

  private void loadIncrementalFile(KVFileType fileType, KVSubmissionType submissionType, DeletionData deletionData) {
    val dataFilePath = kvFileSystem.getDataFilePath(INCREMENTAL_FILE, fileType);
    val referencedType = RELATIONS.get(fileType);
    log.info("{}", repeat("=", 75));
    log.info("Loading incremental file: '{}.{}' ('{}'); Referencing '{}'",
        new Object[] { fileType, submissionType, dataFilePath, referencedType });

    incrementalData.put(
        fileType,
        new KVIncrementalFileDataDigest( // TODO: subclass for referencing/non-referencing?
            kvFileParser,
            getIncrementalFileDescription(
                submissionType.isIncrementalToBeTreatedAsExisting(), fileType, dataFilePath),
            1000000,
            kvFileSystem,
            deletionData,

            referencedType != null ?
                Optional.of(incrementalData.get(referencedType)) : Optional.<KVFileDataDigest> absent(),
            // existingData.get(fileType),
            // existingData.get(RELATIONS.get(fileType)),
            // getOptionalReferencedData(fileType),

            errors.getFileErrors(fileType),
            referencedType != null ?
                Optional.of(errors.getFileErrors(referencedType)) : Optional.<KVFileErrors> absent(),

            surjectivityValidator)
            .processFile());
  }

  private void loadPlaceholderExistingFiles() {
    log.info("Loading placeholder existing files");
    loadPlaceholderExistingFile(DONOR);
    loadPlaceholderExistingFile(SPECIMEN);
    loadPlaceholderExistingFile(SAMPLE);

    for (val dataType : KVExperimentalDataType.values()) {
      for (val fileType : dataType.getFileTypes()) {
        loadPlaceholderExistingFile(fileType);
      }
    }
  }

  private void loadPlaceholderExistingFile(KVFileType fileType) {
    log.info("Loading placeholder existing file: '{}'", fileType);
    existingData.put(
        fileType,
        KVFileDataDigest.getEmptyInstance(kvFileParser, getPlaceholderFileDescription(fileType)));
  }

  @SuppressWarnings("unused")
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

  @SuppressWarnings("unused")
  private void validateComplexSurjection() {
    log.info("{}", repeat("=", 75));
    log.info("Validating complex surjection");
    surjectivityValidator.validateComplexSurjection(
        // existingData.get(SAMPLE),
        surjectivityValidator.getSurjectionExpectedKeys(incrementalData.get(SAMPLE)),
        errors.getFileErrors(SAMPLE));
    log.info("{}", repeat("=", 75));
  }

}
