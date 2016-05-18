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

import static com.google.common.base.Stopwatch.createStarted;
import static com.google.common.collect.Sets.difference;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_ANALYZED_SAMPLE_ID;
import static org.icgc.dcc.common.core.util.Formats.formatCount;
import static org.icgc.dcc.submission.validation.core.ClinicalFields.getSampleSampleId;
import static org.icgc.dcc.submission.validation.pcawg.util.PCAWGFields.isPCAWGSample;

import java.util.List;
import java.util.Set;

import org.icgc.dcc.submission.core.model.Record;
import org.icgc.dcc.submission.core.report.Error;
import org.icgc.dcc.submission.core.report.ErrorType;
import org.icgc.dcc.submission.validation.core.Clinical;
import org.icgc.dcc.submission.validation.core.ReportContext;

import com.google.common.collect.Sets;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of https://wiki.oicr.on.ca/display/DCCREVIEW/PCAWG+Clinical+Field+Requirements
 */
@Slf4j
@RequiredArgsConstructor
public class PCAWGClinicalValidator {

  /**
   * Data.
   */
  @NonNull
  private final Clinical clinical;

  /**
   * Reference.
   */
  @NonNull
  private final Set<String> referenceSampleIds;

  /**
   * State.
   */
  @NonNull
  private final ReportContext context;

  public void execute() {
    val watch = createStarted();
    log.info("Starting clinical validation with {} reference sample ids...", formatCount(referenceSampleIds));

    log.info("Validating sample study...");
    validateSamples(clinical.getCore().getSamples());
    log.info("Finished validating sample study");

    log.info("Finshed validating in {}", watch);
  }

  private void validateSamples(List<Record> samples) {
    validateSampleStudy(samples);
    validateSamplePresence(samples);
  }

  private void validateSampleStudy(List<Record> samples) {
    for (val sample : samples) {
      val sampleId = getSampleSampleId(sample);

      if (isPCAWGSample(sample)) {
        if (!isValidSampleId(sampleId)) {
          reportError(error(sample)
              .type(ErrorType.PCAWG_SAMPLE_STUDY_INVALID)
              .fieldNames(SUBMISSION_ANALYZED_SAMPLE_ID)
              .value(sampleId));
        }
      } else {
        if (isValidSampleId(sampleId)) {
          reportError(error(sample)
              .type(ErrorType.PCAWG_SAMPLE_STUDY_MISSING)
              .fieldNames(SUBMISSION_ANALYZED_SAMPLE_ID)
              .value(sampleId));
        }
      }
    }
  }

  private void validateSamplePresence(List<Record> samples) {
    val sampleIds = Sets.newHashSet();
    for (val sample : samples) {
      val sampleId = getSampleSampleId(sample);

      sampleIds.add(sampleId);
    }

    val missingSampleIds = difference(referenceSampleIds, sampleIds);
    if (!missingSampleIds.isEmpty()) {
      reportError(error(samples.get(0)) // Use first record to get a mandatory file name
          .lineNumber(-1)
          .type(ErrorType.PCAWG_SAMPLE_MISSING)
          .fieldNames(SUBMISSION_ANALYZED_SAMPLE_ID)
          .value(missingSampleIds));
    }
  }

  private boolean isValidSampleId(String sampleId) {
    return referenceSampleIds.contains(sampleId);
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
