/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0. You
 * should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Map;

import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.validation.report.Outcome;
import org.icgc.dcc.validation.report.ReportCollector;
import org.icgc.dcc.validation.report.SchemaReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cascading.flow.Flow;
import cascading.flow.FlowDef;
import cascading.pipe.Pipe;

import com.google.common.collect.Maps;

public abstract class BaseFileSchemaFlowPlanner implements FileSchemaFlowPlanner {

  private static final Logger log = LoggerFactory.getLogger(BaseFileSchemaFlowPlanner.class);

  private final FileSchema fileSchema;

  private final FlowType flowType;

  private final Map<String, Pipe> reports = Maps.newHashMap();

  private final Map<String, ReportCollector> collectors = Maps.newHashMap();

  protected BaseFileSchemaFlowPlanner(FileSchema fileSchema, FlowType flowType) {
    checkArgument(fileSchema != null);
    checkArgument(flowType != null);
    this.fileSchema = fileSchema;
    this.flowType = flowType;
  }

  @Override
  public String getName() {
    return getSchema().getName() + "." + flowType.toString();
  }

  @Override
  public FileSchema getSchema() {
    return fileSchema;
  }

  @Override
  public final void apply(ReportingPlanElement element) {
    Pipe pipe = getTail(element.getName());
    log.info("[{}] applying element [{}]", getName(), element.describe());
    reports.put(element.getName(), element.report(pipe));
    this.collectors.put(element.getName(), element.getCollector());
  }

  protected Pipe getTail(String basename) {
    return getStructurallyValidTail(); // overwritten in the case of the internal version
  }

  @Override
  public Flow<?> connect(CascadingStrategy strategy) {
    FlowDef def = new FlowDef().setName(getName());

    for(Map.Entry<String, Pipe> p : reports.entrySet()) {
      def.addTailSink(p.getValue(), strategy.getReportTap(getSchema(), flowType, p.getKey()));
    }

    onConnect(def, strategy);

    // Make a flow only if there's something to do
    if(def.getSinks().size() > 0 && def.getSources().size() > 0) {
      return strategy.getFlowConnector().connect(def);
    }
    return null;
  }

  @Override
  public Outcome collect(CascadingStrategy strategy, SchemaReport report) {
    Outcome result = Outcome.PASSED;
    for(ReportCollector reportCollector : collectors.values()) {
      Outcome outcome = reportCollector.collect(strategy, report);
      if(outcome == Outcome.FAILED) {
        result = Outcome.FAILED;
      }
    }
    return result;
  }

  protected abstract Pipe getStructurallyValidTail();

  protected abstract Pipe getStructurallyInvalidTail();

  protected abstract FlowDef onConnect(FlowDef flowDef, CascadingStrategy strategy);
}
