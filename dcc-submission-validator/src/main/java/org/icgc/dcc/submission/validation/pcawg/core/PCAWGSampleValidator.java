/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.pcawg.core;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Sets.difference;
import static java.util.stream.Collectors.groupingBy;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_ANALYZED_SAMPLE_ID;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_DONOR_ID;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_SPECIMEN_ID;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;
import static org.icgc.dcc.submission.validation.core.ClinicalFields.getSampleSampleId;
import static org.icgc.dcc.submission.validation.core.ClinicalFields.getSpecimenSpecimenId;
import static org.icgc.dcc.submission.validation.core.ClinicalFields.getSpecimenType;
import static org.icgc.dcc.submission.validation.pcawg.util.PCAWGFields.isPCAWGSample;
import static org.icgc.dcc.submission.validation.sample.util.SampleTypeFields.SPECIMEN_TYPE_FIELD_NAME;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.icgc.dcc.submission.core.model.Record;
import org.icgc.dcc.submission.core.report.Error;
import org.icgc.dcc.submission.core.report.ErrorType;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.validation.core.ClinicalCore;
import org.icgc.dcc.submission.validation.core.ClinicalFields;
import org.icgc.dcc.submission.validation.core.ReportContext;

import com.google.common.collect.ImmutableSet;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of PCAWG sample validation rules.
 * 
 * @see https://wiki.oicr.on.ca/pages/viewpage.action?pageId=66948684.
 * @see https://wiki.oicr.on.ca/display/DCCREVIEW/PCAWG+Clinical+Field+Requirements
 */
@Slf4j
@RequiredArgsConstructor
public class PCAWGSampleValidator {

  /**
   * Metadata.
   */
  @NonNull
  private final CodeList specimenTypes;

  /**
   * Data.
   */
  @NonNull
  private final ClinicalCore clinical;
  @NonNull
  private final List<PCAWGSample> pcawgSamples;

  /**
   * State.
   */
  @NonNull
  private final ReportContext context;

  public void execute() {
    log.info("Starting clinical validation...");
    val watch = createStarted();

    log.info("Validating donors...");
    validateDonorIds(clinical.getDonors());
    log.info("Finished validating donors");

    log.info("Validating specimen...");
    validateSpecimenIds(clinical.getSpecimens());
    validateSpecimenType(clinical.getSpecimens());
    log.info("Finished validating specimen");

    log.info("Validating samples...");
    validateSampleIds(clinical.getSamples());
    validateSampleStudy(clinical.getSamples());
    log.info("Finished validating samples");

    log.info("Finished validating in {}", watch);
  }

  private void validateDonorIds(List<Record> records) {
    validateIds(records, SUBMISSION_DONOR_ID, ErrorType.PCAWG_DONOR_MISSING,
        ClinicalFields::getDonorId, PCAWGSample::getDonorId);
  }

  private void validateSpecimenIds(List<Record> records) {
    validateIds(records, SUBMISSION_SPECIMEN_ID, ErrorType.PCAWG_SPECIMEN_MISSING,
        ClinicalFields::getSpecimenSpecimenId, PCAWGSample::getSpecimenId);
  }

  private void validateSpecimenType(List<Record> specimens) {
    val specimenTypes = this.specimenTypes.getTermsMapping();
    val pcawgSpecimens = pcawgSamples.stream().collect(groupingBy(PCAWGSample::getSpecimenId));

    for (val specimen : specimens) {
      val specimenId = getSpecimenSpecimenId(specimen);
      val values = pcawgSpecimens.get(specimenId);
      val nonPCAWG = values == null;
      if (nonPCAWG) {
        // No need to validate
        continue;
      }

      // All values should bare the same value so pick a representative
      val pcawgSpecimen = values.get(0);

      // Normalize to code and value representations
      val expectedRaw = pcawgSpecimen.getSpecimenType();
      val expectedCode = specimenTypes.containsKey(expectedRaw) ? expectedRaw : specimenTypes.inverse().get(expectedRaw);
      val expectedValue = specimenTypes.get(expectedCode);

      // Normalize to code and value representations
      val actualRaw = getSpecimenType(specimen);
      val actualCode = specimenTypes.containsKey(actualRaw) ? actualRaw : specimenTypes.inverse().get(actualRaw);
      val actualValue = specimenTypes.get(actualCode);

      checkState(expectedCode != null && actualCode != null,
          "Missing specimen type codes to compare. Expected: raw='%s' value='%s' code=%s, Actual: raw='%s' value='%s', code=%s",
          expectedRaw, expectedValue, expectedCode, actualRaw, actualValue, actualCode);

      // Arbitrarily match by code
      val match = expectedCode.equals(actualCode);
      if (!match) {
        reportError(error(specimen)
            .type(ErrorType.PCAWG_SPECIMEN_TYPE_INVALID)
            .fieldNames(SPECIMEN_TYPE_FIELD_NAME)
            .value(String.format("Expected: value='%s' (code=%s), Actual: value='%s' (code=%s)",
                expectedValue, expectedCode, actualValue, actualCode)));
      }
    }
  }

  private void validateSampleIds(List<Record> records) {
    validateIds(records, SUBMISSION_ANALYZED_SAMPLE_ID, ErrorType.PCAWG_SAMPLE_MISSING,
        ClinicalFields::getSampleSampleId, PCAWGSample::getSampleId);
  }

  private void validateSampleStudy(List<Record> samples) {
    val pcawgSampleIds = getIds(pcawgSamples, PCAWGSample::getSampleId);

    for (val sample : samples) {
      val sampleId = getSampleSampleId(sample);
      val pcawgRequired = pcawgSampleIds.contains(sampleId);
      val error = error(sample).fieldNames(SUBMISSION_ANALYZED_SAMPLE_ID).value(sampleId);

      if (isPCAWGSample(sample) && !pcawgRequired) {
        reportError(error.type(ErrorType.PCAWG_SAMPLE_MISSING));
      } else if (pcawgRequired) {
        reportError(error.type(ErrorType.PCAWG_SAMPLE_STUDY_MISSING));
      }
    }
  }

  private void validateIds(List<Record> records, String fieldName, ErrorType errorType,
      Function<Record, String> recordId, Function<PCAWGSample, String> pcawgId) {
    val missingIds = getMissingIds(records, recordId, pcawgId);

    if (!missingIds.isEmpty()) {
      reportError(error(records.get(0)) // Use first record to get a mandatory file name
          .lineNumber(-1)
          .type(errorType)
          .fieldNames(fieldName)
          .value(missingIds));
    }
  }

  private Set<String> getMissingIds(List<Record> records,
      Function<Record, String> getRecordId, Function<PCAWGSample, String> getPcawgId) {
    val ids = getIds(records, getRecordId);
    val pcawgIds = getIds(pcawgSamples, getPcawgId);

    // Will return the subset of expected valid ids that are missing from the actual ids
    return difference(pcawgIds, ids);
  }

  private <T> ImmutableSet<String> getIds(List<T> values, Function<T, String> getId) {
    return values.stream().map(getId).collect(toImmutableSet());
  }

  private void reportError(Error.Builder builder) {
    context.reportError(builder.build());
  }

  private static Error.Builder error(Record record) {
    return Error.error()
        .fileName(record.getFile().getName())
        .lineNumber(record.getLineNumber());
  }

}
