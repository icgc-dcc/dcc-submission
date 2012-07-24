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

import java.util.Map;

import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.validation.report.Outcome;
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

  private final Map<String, Pipe> reports = Maps.newHashMap();

  private ReportingPlanElement reportElement;

  protected BaseFileSchemaFlowPlanner(FileSchema fileSchema) {
    this.fileSchema = fileSchema;
  }

  @Override
  public FileSchema getSchema() {
    return fileSchema;
  }

  @Override
  public String getReportName() {
    return reportElement.getName();
  }

  @Override
  public void apply(ReportingPlanElement element) {
    Pipe split = new Pipe(element.getName(), getTail());
    log.info("[{}] applying element [{}]", getName(), element.describe());
    reports.put(element.getName(), element.report(split));
    this.reportElement = element;
  }

  @Override
  public Flow<?> connect(CascadingStrategy strategy) {
    FlowDef def = new FlowDef().setName(getName());

    for(Map.Entry<String, Pipe> p : reports.entrySet()) {
      def.addTailSink(p.getValue(), strategy.getReportTap(this, p.getKey()));
    }
    return strategy.getFlowConnector().connect(onConnect(def, strategy));
  }

  @Override
  public Outcome collect(CascadingStrategy strategy, SchemaReport report) {
    return reportElement.getCollector().collect(strategy, report);
  }

  protected abstract Pipe getTail();

  protected abstract FlowDef onConnect(FlowDef flowDef, CascadingStrategy strategy);
}
