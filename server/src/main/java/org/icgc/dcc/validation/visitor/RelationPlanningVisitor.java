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
package org.icgc.dcc.validation.visitor;

import java.util.Arrays;
import java.util.Iterator;

import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.model.dictionary.Relation;
import org.icgc.dcc.validation.PlanningVisitor;
import org.icgc.dcc.validation.cascading.ValidationFields;
import org.icgc.dcc.validation.plan.ExternalPlanElement;
import org.icgc.dcc.validation.plan.PlanPhase;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.pipe.CoGroup;
import cascading.pipe.Every;
import cascading.pipe.Pipe;
import cascading.pipe.joiner.LeftJoin;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

public class RelationPlanningVisitor extends PlanningVisitor {

  public RelationPlanningVisitor() {
    super(PlanPhase.EXTERNAL);
  }

  @Override
  public void visit(Relation relation) {
    collect(new RelationPlanElement(getCurrentSchema(), relation));
  }

  private static class RelationPlanElement implements ExternalPlanElement {

    private final String lhs;

    private final String[] lhsFields;

    private final String rhs;

    private final String[] rhsFields;

    private RelationPlanElement(FileSchema fileSchema, Relation relation) {
      this.lhs = fileSchema.getName();
      this.lhsFields = relation.getFields().toArray(new String[] {});
      this.rhs = relation.getOther();
      this.rhsFields = relation.getOtherFields().toArray(new String[] {});
    }

    @Override
    public String describe() {
      return String.format("fk[%s:%s->%s:%s]", lhs, Arrays.toString(lhsFields), rhs, Arrays.toString(rhsFields));
    }

    @Override
    public PlanPhase phase() {
      return PlanPhase.EXTERNAL;
    }

    @Override
    public String[] lhsFields() {
      return lhsFields;
    }

    @Override
    public String rhs() {
      return rhs;
    }

    @Override
    public String[] rhsFields() {
      return rhsFields;
    }

    @Override
    public Pipe join(Pipe lhs, Pipe rhs) {
      String[] renamed = rename();
      Fields merged = Fields.merge(new Fields(lhsFields), new Fields(renamed));
      Pipe pipe = new CoGroup(lhs, new Fields(lhsFields), rhs, new Fields(rhsFields), merged, new LeftJoin());
      pipe = new Every(pipe, merged, new NoNullBuffer(), Fields.RESULTS);
      return pipe;
    }

    private String[] rename() {
      String[] renamed = new String[rhsFields.length];
      for(int i = 0; i < renamed.length; i++) {
        renamed[i] = rhs + "$" + rhsFields[i];
      }
      return renamed;
    }

    private static class NoNullBuffer extends BaseOperation implements Buffer {

      private NoNullBuffer() {
        super(2, new Fields(ValidationFields.STATE_FIELD_NAME));
      }

      @Override
      public void operate(FlowProcess flowProcess, BufferCall bufferCall) {
        Iterator<TupleEntry> iter = bufferCall.getArgumentsIterator();
        while(iter.hasNext()) {
          TupleEntry e = iter.next();
          if(e.getObject(1) == null) {
            bufferCall.getOutputCollector().add(new Tuple("null"));
          }
        }
      }
    }

  }

}
