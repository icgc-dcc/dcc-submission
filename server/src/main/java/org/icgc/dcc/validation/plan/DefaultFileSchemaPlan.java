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

import java.util.List;
import java.util.Map;

import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.validation.Main;

import cascading.flow.Flow;
import cascading.flow.FlowDef;
import cascading.flow.local.LocalFlowConnector;
import cascading.flow.planner.PlannerException;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.Retain;
import cascading.tap.Tap;
import cascading.tuple.Fields;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

class DefaultFileSchemaPlan implements FileSchemaPlan {

  private final Plan plan;

  private final FileSchema fileSchema;

  private final Pipe head;

  private Pipe validTail;

  private final Map<String[], Pipe> trimmedTails = Maps.newHashMap();

  private final List<Pipe> joinedTails = Lists.newLinkedList();

  DefaultFileSchemaPlan(Plan plan, FileSchema fileSchema) {
    checkArgument(plan != null);
    checkArgument(fileSchema != null);
    this.plan = plan;
    this.fileSchema = fileSchema;
    this.validTail = this.head = new Pipe(fileSchema.getName());

    this.validTail = applySystemPipes(this.validTail);
  }

  @Override
  public FileSchema getSchema() {
    return fileSchema;
  }

  @Override
  public void apply(InternalIntegrityPlanElement element) {
    validTail = element.extend(validTail);
  }

  @Override
  public void apply(ExternalIntegrityPlanElement element) {
    Pipe lhs = trim(element.lhsFields());
    Pipe rhs = plan.getPlan(element.rhs()).trim(element.rhsFields());
    joinedTails.add(element.join(lhs, rhs));
  }

  @Override
  public Pipe trim(String... fields) {
    if(trimmedTails.containsKey(fields) == false) {
      Pipe trim = new Retain(head, new Fields(fields));
      trimmedTails.put(fields, trim);
    }
    return trimmedTails.get(fields);
  }

  @Override
  public Flow connect(Tap source, Tap sink) {
    FlowDef def = new FlowDef();
    def.setName(getSchema().getName()).addSource(validTail, source).addSink(validTail, sink).addTail(validTail);
    try {
      return new LocalFlowConnector().connect(source, sink, validTail);
    } catch(PlannerException e) {
      System.err.println(getSchema().getName());
      throw e;
    }
  }

  private Pipe applySystemPipes(Pipe pipe) {
    return new Each(pipe, new Main.AddValidationFieldsFunction(), Fields.ALL);
  }

}
