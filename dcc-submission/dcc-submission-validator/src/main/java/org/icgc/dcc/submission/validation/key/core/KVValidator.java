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
import static org.icgc.dcc.submission.validation.key.core.KVFileDescription.getFileDescription;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SPECIMEN;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.data.KVFileDataDigest;
import org.icgc.dcc.submission.validation.key.data.KVSubmissionDataDigest;
import org.icgc.dcc.submission.validation.key.enumeration.KVExperimentalDataType;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.error.KVFileErrors;
import org.icgc.dcc.submission.validation.key.error.KVSubmissionErrors;
import org.icgc.dcc.submission.validation.key.report.KVReport;
import org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator;

import com.google.common.base.Optional;

@Slf4j
public class KVValidator {

  private static final int DEFAULT_LOG_THRESHOLD = 1000000;

  @NonNull
  private final KVFileParser kvFileParser;
  @NonNull
  private final KVFileSystem kvFileSystem;
  @NonNull
  private final KVReport kvReport;
  @NonNull
  private final SurjectivityValidator surjectivityValidator;
  private final KVSubmissionDataDigest data = new KVSubmissionDataDigest();
  private final KVSubmissionErrors errors = new KVSubmissionErrors();

  public KVValidator(KVFileParser kvFileParser, KVFileSystem kvFileSystem, KVReport kvReport) {
    this.kvFileParser = kvFileParser;
    this.kvFileSystem = kvFileSystem;
    this.kvReport = kvReport;
    this.surjectivityValidator = new SurjectivityValidator(kvFileSystem);
  }

  /**
   * Order matters!
   */
  public void validate() {
    log.info("Loading data");

    // Process clinical data
    log.info("Processing clinical data");
    processFile(DONOR);
    processFile(SPECIMEN);
    processFile(SAMPLE);

    // Process experimental data
    for (val dataType : KVExperimentalDataType.values()) {
      if (kvFileSystem.hasType(dataType)) {
        log.info("Processing '{}' data", dataType);
        for (val fileType : dataType.getFileTypes()) {
          processFile(fileType);
        }
      } else {
        log.info("No '{}' data", dataType);
      }
    }

    log.info("{}", repeat("=", 75));
    for (val entry : data.entrySet()) {
      log.debug("{}: {}", entry.getKey(), entry.getValue());
    }
    log.debug("{}", repeat("=", 75));

    // Surjection validation (can only be done at the very end)
    validateComplexSurjection();

    // Report
    boolean valid = errors.describe(kvReport, data.getFileDescriptions()); // TODO: prettify
    log.info("{}", valid);
    log.info("done.");
  }

  private void processFile(KVFileType fileType) {
    val dataFilePath = kvFileSystem.getDataFilePath(fileType);
    val referencedType = RELATIONS.get(fileType);
    log.info("{}", repeat("=", 75));
    log.info("Loading file: '{}.' ('{}'); Referencing '{}'",
        new Object[] { fileType, dataFilePath, referencedType });

    data.put(
        fileType,
        new KVFileDataDigest( // TODO: subclass for referencing/non-referencing?
            kvFileParser,
            getFileDescription(fileType, dataFilePath),
            DEFAULT_LOG_THRESHOLD,
            referencedType != null ?
                Optional.of(data.get(referencedType)) :
                Optional.<KVFileDataDigest> absent(),
            errors.getFileErrors(fileType),
            referencedType != null ?
                Optional.of(errors.getFileErrors(referencedType)) :
                Optional.<KVFileErrors> absent(),
            surjectivityValidator)
            .processFile());
  }

  @SuppressWarnings("unused")
  private Optional<KVFileDataDigest> getOptionalReferencedData(KVFileType fileType) {
    val referencedFileType = RELATIONS.get(fileType);
    if (referencedFileType == null) {
      log.info("No referenced file type for '{}'", DONOR);
      checkState(fileType == DONOR, "TODO");
      return absent();
    } else {
      if (data.contains(referencedFileType)) { // May not if not re-submitted
        log.info("Data contains '{}'", referencedFileType);
        return of(data.get(referencedFileType));
      } else { // Fall back on existing data
        log.info("Data does not contain '{}', falling back on existing data", referencedFileType);
        return absent();
      }
    }
  }

  private void validateComplexSurjection() {
    log.info("{}", repeat("=", 75));
    log.info("Validating complex surjection");
    surjectivityValidator.validateComplexSurjection(
        // existingData.get(SAMPLE),
        surjectivityValidator.getSurjectionExpectedKeys(data.get(SAMPLE)),
        errors.getFileErrors(SAMPLE));
    log.info("{}", repeat("=", 75));
  }

}
