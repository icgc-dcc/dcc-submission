package org.icgc.dcc.validation.restriction;

import java.util.Arrays;
import java.util.Iterator;

import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.model.dictionary.Relation;
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

public class RelationPlanElement implements ExternalPlanElement {

  private final String[] lhsFields;

  private final String rhs;

  private final String[] rhsFields;

  public RelationPlanElement(FileSchema fileSchema, Relation relation) {
    this.lhsFields = relation.getFields().toArray(new String[] {});
    this.rhs = relation.getOther();
    this.rhsFields = relation.getOtherFields().toArray(new String[] {});
  }

  @Override
  public String describe() {
    return String.format("fk[%s->%s:%s]", Arrays.toString(lhsFields), rhs, Arrays.toString(rhsFields));
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
