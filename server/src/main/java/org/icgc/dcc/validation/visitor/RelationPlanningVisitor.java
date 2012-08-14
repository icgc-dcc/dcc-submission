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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.Relation;
import org.icgc.dcc.validation.ExternalFlowPlanningVisitor;
import org.icgc.dcc.validation.ExternalPlanElement;
import org.icgc.dcc.validation.PlannerException;
import org.icgc.dcc.validation.ValidationErrorCode;
import org.icgc.dcc.validation.cascading.TupleEntryUtils;
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

  static class RelationPlanElement implements ExternalPlanElement {

    private final String lfs;

    private final String[] lhsFields;

    private final String rhs;

    private final String[] rhsFields;

    private final List<Integer> optionals;

    private RelationPlanElement(FileSchema fileSchema, Relation relation) {
      this.lfs = fileSchema.getName();
      this.lhsFields = relation.getFields().toArray(new String[] {});
      this.rhs = relation.getOther();
      this.rhsFields = relation.getOtherFields().toArray(new String[] {});
      this.optionals = relation.getOptionals();
    }

    @Override
    public String describe() {
      return String.format("%s[%s:%s->%s:%s]", NAME, lfs, Arrays.toString(lhsFields), rhs, Arrays.toString(rhsFields));
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
      String[] renamedRhsFields = rename();
      String[] requiredLhsFields = extractRequiredFields(lhsFields);
      String[] requiredRhsFields = extractRequiredFields(rhsFields);
      String[] optionalLhsFields = extractOptionalFields(lhsFields);
      String[] requiredRhsRenamedFields = extractRequiredFields(renamedRhsFields);
      String[] optionalRhsRenamedFields = extractOptionalFields(renamedRhsFields);
      Fields merged = Fields.merge(new Fields(lhsFields), new Fields(renamedRhsFields));
      Pipe pipe =
          new CoGroup(lhsPipe, new Fields(requiredLhsFields), rhsPipe, new Fields(requiredRhsFields), merged,
              new LeftJoin());
      NoNullBuffer noNullBuffer =
          new NoNullBuffer(rhs, lhsFields, rhsFields, requiredLhsFields, requiredRhsRenamedFields, optionalLhsFields,
              optionalRhsRenamedFields);
      pipe = new Every(pipe, merged, noNullBuffer, Fields.RESULTS);
      return pipe;
    }

    private String[] extractOptionalFields(String[] fields) {
      return extractFields(true, fields);
    }

    private String[] extractRequiredFields(String[] fields) {
      return extractFields(false, fields);
    }

    private String[] extractFields(boolean keepOptional, String[] fields) {
      int size = keepOptional ? optionals.size() : fields.length - optionals.size();
      String[] requiredFields = new String[size];
      int j = 0;
      for(int i = 0; i < fields.length; i++) {
        if(optionals.contains(i) == keepOptional) {
          requiredFields[j++] = fields[i];
        }
      }
      return requiredFields;
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

      private final String rhs;

      private final String[] lhsFields;

      private final String[] rhsFields;

      private final String[] requiredLhsFields;

      private final String[] requiredRhsFields;

      private final String[] optionalLhsFields;

      private final String[] optionalRhsFields;

      private final int optionalSize;

      private final boolean conditional;

      NoNullBuffer(String rhs, String[] lhsFields, String[] rhsFields, String[] requiredLhsFields,
          String[] requiredRhsFields, String[] optionalLhsFields, String[] optionalRhsFields) {
        super(lhsFields.length + rhsFields.length, new Fields(ValidationFields.STATE_FIELD_NAME));
        this.rhs = rhs;
        this.lhsFields = lhsFields;
        this.rhsFields = rhsFields;
        this.requiredLhsFields = requiredLhsFields;
        this.requiredRhsFields = requiredRhsFields;
        this.optionalLhsFields = optionalLhsFields;
        this.optionalRhsFields = optionalRhsFields;
        this.optionalSize = optionalLhsFields.length;
        this.conditional = optionalSize > 0;
      }

      @Override
      @SuppressWarnings("unchecked")
      public void operate(FlowProcess flowProcess, BufferCall bufferCall) {
        Iterator<TupleEntry> iter = bufferCall.getArgumentsIterator();

        // potential memory issue discussed in DCC-300
        Set<List<Object>> lhsList = null;
        Set<List<Object>> rhsList = null;
        List<Object> requiredLhsObjects = null;
        if(conditional) {
          lhsList = new HashSet<List<Object>>();
          rhsList = new HashSet<List<Object>>();
          requiredLhsObjects = TupleEntryUtils.getObjects(bufferCall.getGroup(), requiredLhsFields);
        }

        while(iter.hasNext()) {
          TupleEntry tupleEntry = iter.next();

          if(TupleEntryUtils.hasValues(tupleEntry, requiredRhsFields) == false) {
            TupleState tupleState = new TupleState(); // not reporting offset for relations (see DCC-300)
            reportRelationError(tupleState, TupleEntryUtils.getObjects(tupleEntry, lhsFields));
            bufferCall.getOutputCollector().add(new Tuple(tupleState));
          } else if(conditional) {
            if(TupleEntryUtils.hasValues(tupleEntry, optionalLhsFields)) {
              lhsList.add(TupleEntryUtils.getObjects(tupleEntry, optionalLhsFields));
            }
            if(TupleEntryUtils.hasValues(tupleEntry, optionalRhsFields)) {
              rhsList.add(TupleEntryUtils.getObjects(tupleEntry, optionalRhsFields));
            }
          }
        }

        if(conditional) {
          for(List<Object> optionalLhsObjects : lhsList) {
            if(rhsList.contains(optionalLhsObjects) == false) {
              List<Object> offendingLhsObjects =
                  rebuildLhsObjects(requiredLhsFields, optionalLhsFields, requiredLhsObjects, optionalLhsObjects);
              TupleState tupleState = new TupleState(); // not reporting offset for relations (see DCC-300)
              reportRelationError(tupleState, offendingLhsObjects);
              bufferCall.getOutputCollector().add(new Tuple(tupleState));
            }
          }
        }
      }

      private void reportRelationError(TupleState tupleState, List<Object> offendingLhsObjects) {
        tupleState.reportError(ValidationErrorCode.MISSING_RELATION_ERROR, offendingLhsObjects,
            Arrays.asList(lhsFields), rhs, Arrays.asList(rhsFields));
      }

      /*
       * So as to avoid storing it all in memory (memory/computing tradeoff)
       */
      private List<Object> rebuildLhsObjects(String[] requiredLhsFields, String[] optionalLhsFields,
          List<Object> requiredLhsObjects, List<Object> optionalLhsObjects) {

        List<String> disorderedLhsFields = new ArrayList<String>(Arrays.asList(requiredLhsFields));
        disorderedLhsFields.addAll(Arrays.asList(optionalLhsFields));

        List<Object> disorderedLhsObjects = new ArrayList<Object>(requiredLhsObjects);
        disorderedLhsObjects.addAll(optionalLhsObjects);

        List<Object> lhsObjects = new ArrayList<Object>();
        for(String lhsField : lhsFields) {
          int index = disorderedLhsFields.indexOf(lhsField);
          if(index == -1) { // necessarily exists by design but just in case
            throw new PlannerException(String.format("%s is not part of %s", lhsField, disorderedLhsFields));
          }
          lhsObjects.add(disorderedLhsObjects.get(index));
        }
        return lhsObjects;
      }
    }
  }
}
