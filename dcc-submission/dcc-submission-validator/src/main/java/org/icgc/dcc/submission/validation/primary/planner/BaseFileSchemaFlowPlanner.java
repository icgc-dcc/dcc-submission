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
package org.icgc.dcc.submission.validation.primary.planner;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.visitor.BaseDictionaryVisitor;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.ReportingPlanElement;
import org.icgc.dcc.submission.validation.primary.report.ReportCollector;

import cascading.flow.Flow;
import cascading.flow.FlowDef;
import cascading.pipe.Pipe;

import com.google.common.collect.Maps;

@Slf4j
public abstract class BaseFileSchemaFlowPlanner implements FileSchemaFlowPlanner {

  private final FileSchema fileSchema;

  protected final String fileName;

  private final FlowType flowType;

  private final Map<String, Pipe> reportPipes = Maps.newHashMap();

  private final Map<String, ReportCollector> collectors = Maps.newHashMap();

  protected BaseFileSchemaFlowPlanner(
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
    return newArrayList(fileSchema.getFieldNames());
  }

  protected List<String> getRequiredFieldNames() {
    return newArrayList(fileSchema.getRequiredFieldNames());
  }

  protected String getSourcePipeName() {
    return fileName;
  }

  @Override
  public void fileSchemaAccept(BaseDictionaryVisitor visitor) {
    fileSchema.accept(visitor);
  }

  @Override
  public final void apply(ReportingPlanElement element) {
    Pipe reportTailPipe = getReportTailPipe(element.getElementName());
    log.info("[{}] applying element [{}]", getFlowName(), element.describe());
    reportPipes.put(element.getElementName(), element.report(reportTailPipe));
    this.collectors.put(element.getElementName(), element.getCollector());
  }

  protected Pipe getReportTailPipe(String basename) {
    return getStructurallyValidTail(); // overwritten in the case of the internal version
  }

  @Override
  public Flow<?> connect(PlatformStrategy platformStrategy) {
    FlowDef flowDef = new FlowDef().setName(getFlowName());

    for (Map.Entry<String, Pipe> p : reportPipes.entrySet()) {
      flowDef.addTailSink(p.getValue(), platformStrategy.getReportTap2(fileName, flowType, p.getKey()));
    }

    onConnect(flowDef, platformStrategy);

    // Make a flow only if there's something to do
    val hasSourcesAndSinks = flowDef.getSinks().size() > 0
        && flowDef.getSources().size() > 0;
    if (hasSourcesAndSinks) {
      return platformStrategy
          .getFlowConnector()
          .connect(flowDef);
    }

    return null;
  }

  @Override
  public void collect(PlatformStrategy strategy, ReportContext context) {
    for (val reportCollector : collectors.values()) {
      reportCollector.collect(strategy, context);
    }
  }

  /**
   * Returns the name of the current flow planner, which will also be used a {@link Flow} name.
   */
  protected String getFlowName() {
    return format("%s.%s", fileName, flowType);
  }

  protected abstract Pipe getStructurallyValidTail();

  protected abstract Pipe getStructurallyInvalidTail();

  protected abstract FlowDef onConnect(FlowDef flowDef, PlatformStrategy strategy);
}
