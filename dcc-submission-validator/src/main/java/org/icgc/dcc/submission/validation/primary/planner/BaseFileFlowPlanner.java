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
package org.icgc.dcc.submission.validation.primary.planner;

import static java.lang.String.format;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.ReportingPlanElement;
import org.icgc.dcc.submission.validation.primary.report.ReportCollector;
import org.icgc.dcc.submission.validation.primary.visitor.PlanningVisitor;

import cascading.flow.Flow;
import cascading.flow.FlowDef;
import cascading.pipe.Pipe;

import com.google.common.collect.Maps;

@Slf4j
public abstract class BaseFileFlowPlanner implements FileFlowPlanner {

  private final FileSchema fileSchema;

  protected final String fileName;

  private final FlowType flowType;

  private final Map<String, Pipe> reportPipes = Maps.newHashMap();

  private final Map<String, ReportCollector> collectors = Maps.newHashMap();

  protected BaseFileFlowPlanner(
      @NonNull FileSchema fileSchema,
      @NonNull String fileName,
      @NonNull FlowType flowType) {
    this.fileSchema = fileSchema;
    this.fileName = fileName;
    this.flowType = flowType;
  }

  protected String getSchemaName() {
    return fileSchema.getName();
  }

  protected List<String> getFieldNames() {
    return fileSchema.getFieldNames();
  }

  protected List<String> getRequiredFieldNames() {
    return fileSchema.getRequiredFieldNames();
  }

  protected String getSourcePipeName() {
    return fileName;
  }

  /**
   * Returns the name of the current flow planner, which will also be used a {@link Flow} name.
   */
  protected String getFlowName() {
    return format("%s.%s", fileName, flowType);
  }

  @Override
  public void acceptVisitor(PlanningVisitor<?> planningVisitor) {

    // Bind flow planner's file to the planning visitor
    planningVisitor.setFlowPlannerFileName(fileName);

    // Trigger cascade visiting of file schema
    fileSchema.accept(planningVisitor);

    // Un-bind file (out of safety)
    planningVisitor.unsetFlowPlannerFileName();
  }

  @Override
  public final void applyReportingPlanElement(ReportingPlanElement reportingPlanElement) {
    val elementName = reportingPlanElement.getElementName();
    val reportTailPipe = getReportTailPipe(elementName);
    log.info("[{}] applying element [{}]", getFlowName(), reportingPlanElement.describe());

    reportPipes.put(
        elementName,
        reportingPlanElement.report(reportTailPipe));
    collectors.put(
        elementName,
        reportingPlanElement.getCollector());
  }

  protected Pipe getReportTailPipe(String basename) {
    return getStructurallyValidTail(); // overwritten in the case of the row-based version
  }

  @Override
  public Flow<?> connect(SubmissionPlatformStrategy platform) {
    val flowDef = new FlowDef().setName(getFlowName());

    for (Map.Entry<String, Pipe> p : reportPipes.entrySet()) {
      flowDef.addTailSink(p.getValue(), platform.getReportTap(fileName, flowType, p.getKey()));
    }

    onConnect(flowDef, platform);

    // Make a flow only if there's something to do
    val hasSourcesAndSinks = flowDef.getSinks().size() > 0 && flowDef.getSources().size() > 0;
    return hasSourcesAndSinks ? connect(platform, flowDef) : null;
  }

  private Flow<?> connect(SubmissionPlatformStrategy platform, FlowDef flowDef) {
    return platform
        .getFlowConnector()
        .connect(flowDef);
  }

  @Override
  public void collectFileReport(SubmissionPlatformStrategy strategy, ReportContext context) {
    for (val reportCollector : collectors.values()) {
      reportCollector.collect(strategy, context);
    }
  }

  protected abstract Pipe getStructurallyValidTail();

  protected abstract Pipe getStructurallyInvalidTail();

  protected abstract FlowDef onConnect(FlowDef flowDef, SubmissionPlatformStrategy strategy);

}
