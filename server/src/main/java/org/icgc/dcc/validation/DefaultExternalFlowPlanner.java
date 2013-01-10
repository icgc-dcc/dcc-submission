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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.validation.visitor.RelationPlanningVisitor.RelationPlanElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cascading.flow.FlowDef;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

class DefaultExternalFlowPlanner extends BaseFileSchemaFlowPlanner implements ExternalFlowPlanner {

  private static final Logger log = LoggerFactory.getLogger(DefaultInternalFlowPlanner.class);

  private final Plan plan;

  private final Map<Key, Pipe> trimmedHeads = Maps.newHashMap();

  private final List<Pipe> joinedTails = Lists.newLinkedList();

  DefaultExternalFlowPlanner(Plan plan, FileSchema fileSchema) {
    super(fileSchema, FlowType.EXTERNAL);
    checkArgument(plan != null);
    checkArgument(fileSchema != null);
    this.plan = plan;
  }

  @Override
  public void apply(ExternalPlanElement element) {
    checkArgument(element != null);

    String currentFileSchemaName = getSchema().getName();
    String referencedFileSchema = element.rhs();

    String fileName = null;
    try {
      fileName = this.plan.path(getSchema());
    } catch(FileNotFoundException fnfe) {
      throw new PlanningException(fnfe);
    } catch(IOException ioe) {
      throw new PlanningException(ioe);
    }

    InternalFlowPlanner lhsInternalFlow;
    InternalFlowPlanner rhsInternalFlow;
    try {
      lhsInternalFlow = plan.getInternalFlow(currentFileSchemaName);
      rhsInternalFlow = plan.getInternalFlow(referencedFileSchema);
    } catch(MissingFileException e) {
      log.error(String.format("missing corresponding file for %s in relation coming from %s", referencedFileSchema,
          currentFileSchemaName));
      throw new PlanningFileLevelException(fileName, ValidationErrorCode.RELATION_FILE_ERROR, referencedFileSchema);
    }

    if(element instanceof RelationPlanElement) { // FIXME: see DCC-391; lesser of all evils for now, file-level error
                                                 // reporting should be thought through as a whole as we are currently
                                                 // too dependent on visiting FileSchema-ta based on file presence (see
                                                 // visitors' apply() method); visiting dictionary may be the way to go
      RelationPlanElement relationPlanElement = (RelationPlanElement) element;
      for(FileSchema afferentFileSchemata : relationPlanElement.getAfferentFileSchemata()) {
        String afferentFileSchemataName = afferentFileSchemata.getName();
        try {
          plan.getInternalFlow(afferentFileSchemataName);
        } catch(MissingFileException e) { // FIXME: this will only catch the first one (consider DCC-391)
          log.error(String.format("missing corresponding file for %s in relation going to %s", referencedFileSchema,
              currentFileSchemaName));
          throw new PlanningFileLevelException(fileName, ValidationErrorCode.REVERSE_RELATION_FILE_ERROR,
              afferentFileSchemataName);
        }
      }
    }

    log.info("[{}] applying element [{}]", getName(), element.describe());
    Key lhsKey = lhsInternalFlow.addTrimmedOutput(element.lhsFields());
    Key rhsKey = rhsInternalFlow.addTrimmedOutput(element.rhsFields());

    Pipe lhs = getTrimmedHead(lhsKey);
    Pipe rhs = getTrimmedHead(rhsKey);

    joinedTails.add(element.join(lhs, rhs));
  }

  @Override
  protected FlowDef onConnect(FlowDef flowDef, CascadingStrategy strategy) {
    for(Key key : trimmedHeads.keySet()) {
      flowDef.addSource(key.getName(), strategy.getTrimmedTap(key));
    }
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
    if(head == null) {
      head = new Pipe(key.getName());
      trimmedHeads.put(key, head);
    }
    return head;
  }

  private Pipe mergeJoinedTails() {
    return new Merge(joinedTails.toArray(new Pipe[] {}));
  }

}
