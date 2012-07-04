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
import org.icgc.dcc.validation.ValidationErrorCode;
import org.icgc.dcc.validation.ExternalFlowPlanningVisitor;
import org.icgc.dcc.validation.ExternalPlanElement;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.cascading.ValidationFields;

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

/**
 * Creates {@code PlanElement}s for {@code Relation}.
 */
public class RelationPlanningVisitor extends ExternalFlowPlanningVisitor {

  private static final String NAME = "fk";

  @Override
  public void visit(Relation relation) {
    collect(new RelationPlanElement(getCurrentSchema(), relation));
  }

  private static class RelationPlanElement implements ExternalPlanElement {

    private final String[] lhsFields;

    private final String rhs;

    private final String[] rhsFields;

    private RelationPlanElement(FileSchema fileSchema, Relation relation) {
      this.lhsFields = relation.getFields().toArray(new String[] {});
      this.rhs = relation.getOther();
      this.rhsFields = relation.getOtherFields().toArray(new String[] {});
    }

    @Override
    public String describe() {
      return String.format("%s[%s->%s:%s]", NAME, Arrays.toString(lhsFields), rhs, Arrays.toString(rhsFields));
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
    public Pipe join(Pipe lhsPipe, Pipe rhsPipe) {
      String[] renamed = rename();
      Fields merged = Fields.merge(new Fields(lhsFields), new Fields(renamed));
      Pipe pipe = new CoGroup(lhsPipe, new Fields(lhsFields), rhsPipe, new Fields(rhsFields), merged, new LeftJoin());
      pipe = new Every(pipe, merged, new NoNullBuffer(lhsFields, rhs, rhsFields), Fields.RESULTS);
      return pipe;
    }

    private String[] rename() {
      String[] renamed = new String[rhsFields.length];
      for(int i = 0; i < renamed.length; i++) {
        renamed[i] = rhs + "$" + rhsFields[i];
      }
      return renamed;
    }

    @SuppressWarnings("rawtypes")
    static class NoNullBuffer extends BaseOperation implements Buffer {

      private final String[] lhsFields;

      private final String rhs;

      private final String[] rhsFields;

      NoNullBuffer(String[] lhsFields, String rhs, String[] rhsFields) {
        super(2, new Fields(ValidationFields.STATE_FIELD_NAME));
        this.lhsFields = lhsFields;
        this.rhs = rhs;
        this.rhsFields = rhsFields;
      }

      @Override
      @SuppressWarnings("unchecked")
      public void operate(FlowProcess flowProcess, BufferCall bufferCall) {
        Iterator<TupleEntry> iter = bufferCall.getArgumentsIterator();
        while(iter.hasNext()) {
          TupleEntry tupleEntry = iter.next();
          if(tupleEntry.getObject(1) == null) {
            String unmatchedValue = tupleEntry.getString(0);
            TupleState tupleState = new TupleState();
            tupleState.reportError(ValidationErrorCode.MISSING_RELATION_ERROR, unmatchedValue, Arrays.asList(lhsFields), rhs,
                Arrays.asList(rhsFields));
            bufferCall.getOutputCollector().add(new Tuple(tupleState));
          }
        }
      }
    }
  }
}
