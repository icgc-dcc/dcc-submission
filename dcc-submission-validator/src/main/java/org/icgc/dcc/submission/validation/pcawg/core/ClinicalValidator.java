/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static com.google.common.collect.Maps.uniqueIndex;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_ANALYZED_SAMPLE_ID;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_DONOR_ID;
import static org.icgc.dcc.common.core.model.SpecialValue.NOT_APPLICABLE_CODE;
import static org.icgc.dcc.common.core.util.FormatUtils.formatCount;
import static org.icgc.dcc.common.core.util.Jackson.DEFAULT;
import static org.icgc.dcc.submission.validation.pcawg.core.ClinicalFields.getDonorDonorId;
import static org.icgc.dcc.submission.validation.pcawg.core.ClinicalFields.getDonorId;
import static org.icgc.dcc.submission.validation.pcawg.core.ClinicalFields.getSampleSampleId;
import static org.icgc.dcc.submission.validation.pcawg.core.ClinicalFields.getSpecimenSpecimenId;
import static org.icgc.dcc.submission.validation.pcawg.util.PCAWGSamples.isPCAWGSample;
import static org.icgc.dcc.submission.validation.util.Streams.filter;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.model.Record;
import org.icgc.dcc.submission.core.report.Error;
import org.icgc.dcc.submission.core.report.ErrorType;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.pcawg.util.TCGAClient;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

@Slf4j
@RequiredArgsConstructor
public class ClinicalValidator {

  /**
   * Constants.
   */
  private static final String RULES_FILE = "pcawg-clinical-rules.json";

  /**
   * Data.
   */
  @NonNull
  private final Clinical clinical;
  @NonNull
  private final ClinicalIndex index;
  private final List<ClinicalRule> rules = readRules();
  private final boolean tcga;

  /**
   * Reference.
   */
  @NonNull
  private final TCGAClient tcgaClient;
  @NonNull
  private final Collection<String> referenceSampleIds;

  /**
   * State.
   */
  @NonNull
  private final ReportContext context;

  public void validate() {
    val watch = createStarted();
    log.info("Starting {} clinical validation with {} reference sample ids...", tcga ? "TCGA" : "non-TCGA",
        formatCount(referenceSampleIds));

    log.info("Resolving PCAWG data...");
    val samples = getPCAWGSamples();
    val specimens = getPCAWGSpecimens();
    val donors = getPCAWGDonors();
    val donorIds = getPCAWGDonorIds(donors);
    val family = getPCAWGOptional(clinical.getFamily(), donorIds);
    val exposure = getPCAWGOptional(clinical.getExposure(), donorIds);
    val therapy = getPCAWGOptional(clinical.getTherapy(), donorIds);
    log.info("Finished resolving PCAWG data");

    log.info("Validating core clinical...");
    validateCore(samples, specimens, donors);
    log.info("Finished validating core clinical");

    log.info("Validating optional clinical...");
    validateOptional(donorIds, family, exposure, therapy);
    log.info("Finished validating optional clinical");

    log.info("Finshed validating in {}", watch);
  }

  private void validateCore(List<Record> samples, List<Record> specimens, List<Record> donors) {
    validateSamples(samples);

    validateFileTypeRules(samples, FileType.SAMPLE_TYPE);
    validateFileTypeRules(specimens, FileType.SPECIMEN_TYPE);
    validateFileTypeRules(donors, FileType.DONOR_TYPE);
  }

  private void validateOptional(Set<String> donorIds, List<Record> family, List<Record> exposure, List<Record> therapy) {
    validateFileTypeDonorPresence(family, FileType.FAMILY_TYPE, donorIds);
    validateFileTypeDonorPresence(exposure, FileType.EXPOSURE_TYPE, donorIds);
    validateFileTypeDonorPresence(therapy, FileType.THERAPY_TYPE, donorIds);

    validateFileTypeRules(family, FileType.FAMILY_TYPE);
    validateFileTypeRules(exposure, FileType.EXPOSURE_TYPE);
    validateFileTypeRules(therapy, FileType.THERAPY_TYPE);
  }

  private void validateSamples(List<Record> samples) {
    for (val sample : samples) {
      validateSample(sample);
    }
  }

  private void validateSample(Record sample) {
    val sampleId = getSampleSampleId(sample);
    val resolvedSampleId = resolveSampleId(sampleId);

    if (!isValidSampleId(resolvedSampleId)) {
      reportError(error(sample)
          .type(ErrorType.PCAWG_SAMPLE_STUDY_MISMATCH)
          .fieldNames(SUBMISSION_ANALYZED_SAMPLE_ID)
          .value(sampleId)
          .params(resolvedSampleId));
    }
  }

  private void validateFileTypeRules(List<Record> records, FileType fileType) {
    val rules = getFileTypeRules(fileType);

    for (val record : records) {
      for (val rule : rules) {
        val applicable = !tcga || tcga && rule.isTcga();
        if (!applicable) {
          continue;
        }

        val fieldName = rule.getFieldName();
        val fieldValue = record.get(rule.getFieldName());

        val normalized = nullToEmpty(fieldValue).trim();
        val invalid = isNullOrEmpty(normalized) || NOT_APPLICABLE_CODE.equals(normalized);
        if (invalid) {
          reportError(error(record)
              .fieldNames(fieldName)
              .value(fieldValue)
              .type(ErrorType.PCAWG_CLINICAL_FIELD_REQUIRED));
        }
      }
    }
  }

  private void validateFileTypeDonorPresence(List<Record> records, FileType fileType, Set<String> donorIds) {
    val recordMap = uniqueIndex(records, record -> getDonorId(record));
    for (val donorId : donorIds) {
      val invalid = !recordMap.containsKey(donorId);
      if (invalid) {
        val donor = index.getDonor(donorId);

        reportError(error(donor)
            .fieldNames(SUBMISSION_DONOR_ID)
            .value(donorId)
            .params(fileType)
            .type(ErrorType.PCAWG_CLINICAL_ROW_REQUIRED));
      }
    }
  }

  private boolean isValidSampleId(String sampleId) {
    return referenceSampleIds.contains(sampleId);
  }

  private String resolveSampleId(String sampleId) {
    return tcga ? tcgaClient.getUUID(sampleId) : sampleId;
  }

  private List<Record> getPCAWGSamples() {
    return filter(clinical.getSamples(), sample -> isPCAWGSample(sample));
  }

  private List<Record> getPCAWGSpecimens() {
    return filter(clinical.getSpecimens(), specimen -> {
      for (Record sample : index.getSpecimenSamples(getSpecimenSpecimenId(specimen))) {
        if (isPCAWGSample(sample)) {
          return true;
        }
      }

      return false;
    });
  }

  private List<Record> getPCAWGDonors() {
    return filter(clinical.getDonors(), donor -> {
      for (Record sample : index.getDonorSamples(getDonorDonorId(donor))) {
        if (isPCAWGSample(sample)) {
          return true;
        }
      }

      return false;
    });
  }

  private Set<java.lang.String> getPCAWGDonorIds(List<Record> donors) {
    val donorIds = Sets.<String> newTreeSet();
    for (val donor : donors) {
      val donorId = getDonorDonorId(donor);
  
      donorIds.add(donorId);
    }
  
    return donorIds;
  }

  private List<Record> getPCAWGOptional(List<Record> records, Set<String> donorIds) {
    return filter(records, record -> donorIds.contains(getDonorId(record)));
  }

  private List<ClinicalRule> getFileTypeRules(FileType fileType) {
    return filter(rules, rule -> fileType == rule.getFileType());
  }

  @SneakyThrows
  public static List<ClinicalRule> readRules() {
    val node = DEFAULT.readTree(Resources.getResource(RULES_FILE));
    val values = node.get("rules");

    return DEFAULT.convertValue(values, new TypeReference<List<ClinicalRule>>() {});
  }

  private Error.Builder error(Record record) {
    return Error
        .error()
        .fileName(record.getFile().getName())
        .lineNumber(record.getLineNumber());
  }

  private void reportError(Error.Builder builder) {
    context.reportError(builder.build());
  }

}
