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
package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map;

import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.validation.report.Outcome;
import org.icgc.dcc.validation.report.SchemaReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cascading.flow.Flow;
import cascading.flow.FlowDef;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

class DefaultExternalFlowPlanner extends BaseFileSchemaFlowPlanner implements ExternalFlowPlanner {

  private static final Logger log = LoggerFactory.getLogger(DefaultInternalFlowPlanner.class);

  private final Plan plan;

  private final FileSchema fileSchema;

  private final Map<Trim, Pipe> trimmedHeads = Maps.newHashMap();

  private final List<Pipe> joinedTails = Lists.newLinkedList();

  DefaultExternalFlowPlanner(Plan plan, FileSchema fileSchema) {
    super(fileSchema);
    checkArgument(plan != null);
    checkArgument(fileSchema != null);
    this.plan = plan;
    this.fileSchema = fileSchema;
  }

  @Override
  public String getName() {
    return getSchema().getName() + ".external";
  }

  @Override
  public FileSchema getSchema() {
    return fileSchema;
  }

  @Override
  public void apply(ExternalPlanElement element) {
    checkArgument(element != null);
    log.info("[{}] applying element [{}]", getName(), element.describe());
    Trim trimLhs = plan.getInternalFlow(getSchema().getName()).addTrimmedOutput(element.lhsFields());
    Trim trimRhs = plan.getInternalFlow(element.rhs()).addTrimmedOutput(element.rhsFields());

    Pipe lhs = getTrimmedHead(trimLhs);
    Pipe rhs = getTrimmedHead(trimRhs);

    joinedTails.add(element.join(lhs, rhs));
  }

  @Override
  public Flow<?> connect(CascadingStrategy strategy) {
    if(joinedTails.size() > 0) {
      return super.connect(strategy);
    }
    return null;
  }

  @Override
  protected FlowDef onConnect(FlowDef flowDef, CascadingStrategy strategy) {
    for(Trim trim : trimmedHeads.keySet()) {
      flowDef.addSource(trim.getName(), strategy.getTrimmedTap(trim));
    }
    return flowDef;
  }

  @Override
  protected Pipe getTail() {
    return mergeJoinedTails();
  }

  private Pipe getTrimmedHead(Trim trim) {
    Pipe head = trimmedHeads.get(trim);
    if(head == null) {
      head = new Pipe(trim.getName());
      trimmedHeads.put(trim, head);
    }
    return head;
  }

  private Pipe mergeJoinedTails() {
    return new Merge(joinedTails.toArray(new Pipe[] {}));
  }

  @Override
  public Outcome collect(CascadingStrategy strategy, SchemaReport report) {
    // TODO Auto-generated method stub
    return null;
  }

}
