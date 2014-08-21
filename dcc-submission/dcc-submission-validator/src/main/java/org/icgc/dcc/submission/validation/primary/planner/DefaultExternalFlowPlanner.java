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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.submission.validation.primary.core.FlowType.EXTERNAL;

import java.util.List;
import java.util.Map;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;
import org.icgc.dcc.submission.validation.primary.core.ExternalPlanElement;
import org.icgc.dcc.submission.validation.primary.core.Key;
import org.icgc.dcc.submission.validation.primary.core.Plan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cascading.flow.FlowDef;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

class DefaultExternalFlowPlanner extends BaseFileFlowPlanner implements ExternalFlowPlanner {

  private static final Logger log = LoggerFactory.getLogger(DefaultInternalFlowPlanner.class);

  private final Plan plan;

  private final Map<Key, Pipe> trimmedHeads = Maps.newHashMap();

  private final List<Pipe> joinedTails = Lists.newLinkedList();

  DefaultExternalFlowPlanner(Plan plan, FileSchema fileSchema) {
    super(fileSchema, null, EXTERNAL); // FIXME: provide file name if ever re-enabled
    checkArgument(plan != null);
    checkArgument(fileSchema != null);
    this.plan = plan;
  }

  @Override
  public void applyExternalPlanElement(ExternalPlanElement element) {
    checkState(false, "Should not be used");
    checkArgument(element != null);

    String currentFileSchemaName = getSchemaName();
    String referencedFileSchema = element.rhs();

    InternalFlowPlanner lhsInternalFlow = plan.getInternalFlow(currentFileSchemaName);
    InternalFlowPlanner rhsInternalFlow = plan.getInternalFlow(referencedFileSchema);

    log.info("[{}] applying element [{}]", getFlowName(), element.describe());
    Key lhsKey = lhsInternalFlow.addTrimmedOutput(element.lhsFields());
    Key rhsKey = rhsInternalFlow.addTrimmedOutput(element.rhsFields());

    Pipe lhs = getTrimmedHead(lhsKey);
    Pipe rhs = getTrimmedHead(rhsKey);

    joinedTails.add(element.join(lhs, rhs));
  }

  @Override
  protected FlowDef onConnect(FlowDef flowDef, SubmissionPlatformStrategy strategy) {
    checkState(false, "Should not be used");
    return flowDef;
  }

  @Override
  protected Pipe getStructurallyValidTail() {
    return mergeJoinedTails();
  }

  @Override
  protected Pipe getStructurallyInvalidTail() {
    throw new IllegalStateException("method should not be used in the context of external validation");
  }

  private Pipe getTrimmedHead(Key key) {
    Pipe head = trimmedHeads.get(key);
    if (head == null) {
      head = new Pipe(key.getName());
      trimmedHeads.put(key, head);
    }
    return head;
  }

  private Pipe mergeJoinedTails() {
    return new Merge(joinedTails.toArray(new Pipe[] {}));
  }

}
