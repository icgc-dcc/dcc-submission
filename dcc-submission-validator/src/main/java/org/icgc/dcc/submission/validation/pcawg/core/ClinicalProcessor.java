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

import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_ANALYZED_SAMPLE_ID;
import static org.icgc.dcc.common.core.util.FormatUtils.formatCount;
import static org.icgc.dcc.submission.validation.pcawg.core.ClinicalFields.getSampleSampleId;
import static org.icgc.dcc.submission.validation.pcawg.util.PanCancer.isPanCancerSample;
import static org.icgc.dcc.submission.validation.util.Streams.filter;

import java.util.Collection;
import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.report.Error;
import org.icgc.dcc.submission.validation.core.Record;
import org.icgc.dcc.submission.validation.core.ReportContext;

@Slf4j
@RequiredArgsConstructor
public class ClinicalProcessor {

  /**
   * Data.
   */
  @NonNull
  private final Clinical clinical;
  private final ClinicalIndex index = createIndex();

  /**
   * Reference.
   */
  @NonNull
  private final Collection<String> referenceSampleIds;

  /**
   * State.
   */
  @NonNull
  private final ReportContext context;

  public void process() {
    val panCancerSamples = getPanCancerSamples();

    log.info("Processing {} PanCancer samples... {}", formatCount(panCancerSamples));
    for (val sample : panCancerSamples) {
      processSample(sample);
    }
  }

  private void processSample(Record sample) {
    val sampleId = getSampleSampleId(sample);

    if (!isValidSampleId(sampleId)) {
      // Assert
      reportError(error(sample)
          .fieldNames(SUBMISSION_ANALYZED_SAMPLE_ID)
          .value(sampleId));
    }

    val specimen = index.getSampleSpecimen(sampleId);

    // TODO: Assert
    reportError(error(specimen));

    val donor = index.getSampleDonor(sampleId);

    // TODO: Assert
    reportError(error(donor));
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

  private ClinicalIndex createIndex() {
    return new ClinicalIndex(clinical);
  }

  private boolean isValidSampleId(String sampleId) {
    return referenceSampleIds.contains(sampleId);
  }

  private List<Record> getPanCancerSamples() {
    return filter(clinical.getSamples(), sample -> isPanCancerSample(sample));
  }

}
