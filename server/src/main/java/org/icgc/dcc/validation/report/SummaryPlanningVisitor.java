/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation.report;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.SummaryType;
import org.icgc.dcc.validation.FlowType;
import org.icgc.dcc.validation.ReportingFlowPlanningVisitor;
import org.icgc.dcc.validation.report.AggregateReportingPlanElement.AveragePlanElement;
import org.icgc.dcc.validation.report.AggregateReportingPlanElement.CompletenessPlanElement;
import org.icgc.dcc.validation.report.AggregateReportingPlanElement.MinMaxPlanElement;

public class SummaryPlanningVisitor extends ReportingFlowPlanningVisitor {

  public SummaryPlanningVisitor() {
    super(FlowType.INTERNAL);
  }

  @Override
  public void visit(FileSchema fileSchema) {
    super.visit(fileSchema);
    Map<SummaryType, List<Field>> summaryTypeToFields = buildSummaryTypeToFields(fileSchema);
    collectElements(fileSchema.getName(), summaryTypeToFields);
  }

  /**
   * Builds a map that associates each {@code SummaryType} with a list of corresponding {@code Field} from the
   * {@code FileSchema}
   */
  private Map<SummaryType, List<Field>> buildSummaryTypeToFields(FileSchema fileSchema) {
    Map<SummaryType, List<Field>> summaryTypeToFields = new LinkedHashMap<SummaryType, List<Field>>();
    for(SummaryType summaryType : SummaryType.values()) {
      for(Field field : fileSchema.getFields()) {
        SummaryType summaryTypeTmp = field.getSummaryType();
        if(summaryTypeTmp == null) {
          summaryTypeTmp = SummaryType.COMPLETENESS;// TODO: to be removed soon
        }

        if(summaryType == summaryTypeTmp) {
          List<Field> list = summaryTypeToFields.get(summaryType);
          if(list == null) {
            list = new ArrayList<Field>();
            summaryTypeToFields.put(summaryType, list);
          }
          list.add(field);
        }
      }
    }
    return summaryTypeToFields;
  }

  /**
   * Collects element based on the {@code Field}'s {@code SummaryType}, so they can later be applied
   */
  private void collectElements(String schemaName, Map<SummaryType, List<Field>> summaryTypeToFields) {
    for(SummaryType summaryType : summaryTypeToFields.keySet()) {
      List<Field> fields = summaryTypeToFields.get(summaryType);
      switch(summaryType) {
      case COMPLETENESS: // TODO: to be removed soon
        collect(new CompletenessPlanElement(schemaName, fields));
        break;
      case AVERAGE:
        collect(new AveragePlanElement(schemaName, fields));
        break;
      case MIN_MAX:
        collect(new MinMaxPlanElement(schemaName, fields));
        break;
      case FREQUENCY:
        collect(new FrequencyPlanElement(schemaName, fields));
        break;
      default:
        break;
      }
    }
  }
}
