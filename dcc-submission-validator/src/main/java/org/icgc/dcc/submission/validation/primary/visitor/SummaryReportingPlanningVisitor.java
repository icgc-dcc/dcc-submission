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
package org.icgc.dcc.submission.validation.primary.visitor;

import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.lang.String.format;
import static org.icgc.dcc.submission.validation.primary.core.FlowType.ROW_BASED;

import java.util.LinkedHashMap;
import java.util.Map;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.SummaryType;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;
import org.icgc.dcc.submission.validation.primary.report.FieldStatDigest;
import org.icgc.dcc.submission.validation.primary.report.FrequencyPlanElement;
import org.icgc.dcc.submission.validation.primary.report.SummaryPlanElement;
import org.icgc.dcc.submission.validation.primary.report.UniqueCountPlanElement;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import lombok.NonNull;
import lombok.val;

public class SummaryReportingPlanningVisitor extends ReportingPlanningVisitor {

  public SummaryReportingPlanningVisitor(@NonNull String projectKey, @NonNull SubmissionPlatformStrategy platform) {
    super(projectKey, platform, ROW_BASED);
  }

  @Override
  public void visit(FileSchema fileSchema) {
    super.visit(fileSchema);

    collectPlanElements(
        getCurrentFileName(),
        getFieldStatsData(fileSchema)); // TODO: create dedicated object for that?
  }

  /**
   * Returns a map associating each {@code SummaryType} with a map of corresponding field name to field digests.
   */
  public ImmutableMap<Optional<SummaryType>, Map<String, FieldStatDigest>> getFieldStatsData(FileSchema fileSchema) {
    val fieldStatsData = new LinkedHashMap<Optional<SummaryType>, Map<String, FieldStatDigest>>();
    for (val field : fileSchema.getFields()) {
      val optionalSummaryType =
          field.getSummaryType() == null ? Optional.<SummaryType> absent() : Optional.of(field.getSummaryType());
      Map<String, FieldStatDigest> fieldStatsDigests = fieldStatsData.get(optionalSummaryType);
      if (fieldStatsDigests == null) {
        fieldStatsDigests = newLinkedHashMap();
        fieldStatsData.put(
            optionalSummaryType,
            fieldStatsDigests);
      }
      fieldStatsDigests.put(
          field.getName(),
          FieldStatDigest.from(field));
    }
    return ImmutableMap.<Optional<SummaryType>, Map<String, FieldStatDigest>> copyOf(fieldStatsData);
  }

  /**
   * Collects element based on the {@code Field}'s {@code SummaryType}, so they can later be applied
   */
  private void collectPlanElements(String fileName,
      Map<Optional<SummaryType>, Map<String, FieldStatDigest>> fieldStatsData) {

    val flowType = getFlowType();
    for (val optionalSummaryType : fieldStatsData.keySet()) {
      val fieldStatDigests = fieldStatsData.get(optionalSummaryType);
      if (optionalSummaryType.isPresent()) {
        switch (optionalSummaryType.get()) {
        case AVERAGE:
          collectPlanElement(new SummaryPlanElement.AveragePlanElement(
              flowType, fileName, fieldStatDigests));
          break;
        case MIN_MAX:
          collectPlanElement(new SummaryPlanElement.MinMaxPlanElement(
              flowType, fileName, fieldStatDigests));
          break;
        case FREQUENCY:
          collectPlanElement(new FrequencyPlanElement(
              flowType, fileName, fieldStatDigests));
          break;
        case UNIQUE_COUNT:
          collectPlanElement(new UniqueCountPlanElement(
              flowType, fileName, fieldStatDigests));
          break;
        default:
          throw new IllegalStateException(
              format("Unknown summary type: '%s'", optionalSummaryType.get()));
        }
      } else {
        collectPlanElement(new SummaryPlanElement.CompletenessPlanElement(
            flowType, fileName, fieldStatDigests));
      }
    }
  }

}
