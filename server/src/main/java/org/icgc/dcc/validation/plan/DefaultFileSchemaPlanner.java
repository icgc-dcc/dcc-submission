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
package org.icgc.dcc.validation.plan;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.validation.Main;
import org.icgc.dcc.validation.cascading.TupleStates;
import org.icgc.dcc.validation.cascading.ValidationFields;

import cascading.flow.FlowDef;
import cascading.pipe.Each;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.Retain;
import cascading.tap.Tap;
import cascading.tuple.Fields;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

class DefaultFileSchemaPlanner implements FileSchemaPlanner {

  private final Planner planner;

  private final FileSchema fileSchema;

  private final Pipe head;

  private final Map<List<String>, Pipe> trimmedTails = Maps.newHashMap();

  private final Map<String, String[]> trimmedHeads = Maps.newHashMap();

  private final List<Pipe> joinedTails = Lists.newLinkedList();

  private final List<FileSchema> parents = Lists.newLinkedList();

  private Pipe validTail;

  DefaultFileSchemaPlanner(Planner plan, FileSchema fileSchema) {
    checkArgument(plan != null);
    checkArgument(fileSchema != null);
    this.planner = plan;
    this.fileSchema = fileSchema;
    this.validTail = this.head = new Pipe(fileSchema.getName());

    this.validTail = applySystemPipes(this.validTail);
  }

  @Override
  public FileSchema getSchema() {
    return fileSchema;
  }

  @Override
  public Iterable<FileSchema> dependsOn() {
    return parents;
  }

  @Override
  public void apply(InternalIntegrityPlanElement element) {
    validTail = element.extend(validTail);
  }

  @Override
  public void apply(ExternalIntegrityPlanElement element) {
    Pipe lhs = trim(element.lhsFields());
    Pipe rhs = planner.getSchemaPlan(element.rhs()).trim(element.rhsFields());
    checkState(lhs != null);
    checkState(rhs != null);
    trimmedHeads.put(fileSchema.getName(), element.lhsFields());
    trimmedHeads.put(element.rhs(), element.rhsFields());
    joinedTails.add(element.join(lhs, rhs));
    parents.add(planner.getSchemaPlan(element.rhs()).getSchema());
  }

  @Override
  public Pipe trim(String... fields) {
    List<String> key = Arrays.asList(fields);
    if(trimmedTails.containsKey(key) == false) {
      Pipe newHead = new Pipe(fileSchema.getName() + ":" + Joiner.on("-").join(fields), head);
      Pipe trim = new Retain(newHead, new Fields(fields));
      trimmedTails.put(key, trim);
    }
    return trimmedTails.get(key);
  }

  @Override
  public FlowDef internalFlow() {
    CascadingStrategy strategy = planner.getCascadingStrategy();
    Pipe tail = applyFilter(validTail);
    Tap source = strategy.getSourceTap(fileSchema);
    Tap sink = strategy.getInternalSinkTap(fileSchema.getName());

    FlowDef def = new FlowDef().setName(getSchema().getName() + ".int").addSource(head, source).addTailSink(tail, sink);
    for(Map.Entry<List<String>, Pipe> e : trimmedTails.entrySet()) {
      def.addTailSink(e.getValue(), strategy.getTrimmedTap(fileSchema.getName(), e.getKey().toArray(new String[] {})));
    }
    return def;
  }

  @Override
  public FlowDef externalFlow() {
    CascadingStrategy strategy = planner.getCascadingStrategy();
    if(joinedTails.size() > 0) {
      Tap sink = strategy.getExternalSinkTap(fileSchema.getName());
      FlowDef def = new FlowDef().setName(getSchema().getName() + ".ext").addTailSink(mergeJoinedTails(), sink);

      for(Map.Entry<String, String[]> e : trimmedHeads.entrySet()) {
        def.addSource(e.getKey(), strategy.getTrimmedTap(e.getKey(), e.getValue()));
      }
      return def;
    }
    return null;
  }

  private Pipe applySystemPipes(Pipe pipe) {
    return new Each(pipe, new Main.AddValidationFieldsFunction(), Fields.ALL);
  }

  private Pipe applyFilter(Pipe pipe) {
    return new Retain(new Each(pipe, TupleStates.keepInvalidTuplesFilter()), ValidationFields.STATE_FIELD);
  }

  private Pipe mergeJoinedTails() {
    return new Merge(joinedTails.toArray(new Pipe[] {}));
  }

}
