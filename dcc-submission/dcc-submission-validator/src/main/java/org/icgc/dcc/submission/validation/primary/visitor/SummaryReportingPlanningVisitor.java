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

import static java.lang.String.format;
import static org.icgc.dcc.submission.validation.primary.core.FlowType.INTERNAL;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.SummaryType;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.primary.report.FrequencyPlanElement;
import org.icgc.dcc.submission.validation.primary.report.SummaryPlanElement;
import org.icgc.dcc.submission.validation.primary.report.UniqueCountPlanElement;

import com.google.common.base.Optional;

public class SummaryReportingPlanningVisitor extends ReportingPlanningVisitor {

  public SummaryReportingPlanningVisitor(@NonNull PlatformStrategy platform) {
    super(platform, INTERNAL);
  }

  @Override
  public void visit(FileSchema fileSchema) {
    super.visit(fileSchema);
    for (val fileName : listMatchingFiles(fileSchema.getPattern())) {
      collectElements(
          fileSchema,
          fileName,
          fileSchema.getSummaryTypes());
    }
  }

  /**
   * Collects element based on the {@code Field}'s {@code SummaryType}, so they can later be applied
   */
  private void collectElements(FileSchema fileSchema, String fileName,
      Map<Optional<SummaryType>, List<String>> summaryTypes) {

    val flowType = getFlowType();
    for (val optionalSummaryType : summaryTypes.keySet()) {
      val fieldNames = summaryTypes.get(optionalSummaryType);
      if (optionalSummaryType.isPresent()) {
        switch (optionalSummaryType.get()) {
        case AVERAGE:
          collectReportingPlanElement(new SummaryPlanElement.AveragePlanElement(
              fileSchema, fileName, fieldNames, flowType));
          break;
        case MIN_MAX:
          collectReportingPlanElement(new SummaryPlanElement.MinMaxPlanElement(
              fileSchema, fileName, fieldNames, flowType));
          break;
        case FREQUENCY:
          collectReportingPlanElement(new FrequencyPlanElement(
              fileSchema, fileName, fieldNames, flowType));
          break;
        case UNIQUE_COUNT:
          collectReportingPlanElement(new UniqueCountPlanElement(
              fileSchema, fileName, fieldNames, flowType));
          break;
        default:
          throw new IllegalStateException(format("Unknown summary type: '{}'", optionalSummaryType.get()));
        }
      } else {
        collectReportingPlanElement(new SummaryPlanElement.CompletenessPlanElement(
            fileSchema, fileName, fieldNames, flowType));
        continue;
      }
    }
  }

}
