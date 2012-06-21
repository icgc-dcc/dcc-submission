package org.icgc.dcc.validation.restriction;

import java.util.Iterator;
import java.util.List;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.RestrictionTypeSchema;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.cascading.ValidationFields;
import org.icgc.dcc.validation.plan.BaseInternalIntegrityPlanElement;
import org.icgc.dcc.validation.plan.PlanElement;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.operation.OperationCall;
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.collect.ImmutableList;
import com.mongodb.DBObject;

public class UniqueFieldsRestriction extends BaseInternalIntegrityPlanElement {

  private static final String NAME = "unique";

  private final List<String> fields;

  private UniqueFieldsRestriction(String[] fields) {
    this.fields = ImmutableList.copyOf(fields);
  }

  @Override
  public String describe() {
    return String.format("unique[%s]", fields);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    Fields groupFields = new Fields(fields.toArray(new String[] {}));
    pipe = new GroupBy(pipe, groupFields);
    pipe = new Every(pipe, Fields.ALL, new CountBuffer(), Fields.RESULTS);

    // These don't work because you can only obtain Fields.GROUP or Fields.VALUES, but not both
    // pipe = new CountBy(pipe, groupFields, new Fields("count"));
    // pipe = new Each(pipe, new ValidationFields("count"), new CountIsOne(), Fields.REPLACE);
    // pipe = new Discard(pipe, new Fields("count"));
    return pipe;
  }

  public static class Type implements RestrictionType {

    @Override
    public String getType() {
      return NAME;
    }

    @Override
    public RestrictionTypeSchema getSchema() {
      return null;
    }

    @Override
    public boolean builds(String name) {
      return NAME.equals(name);
    }

    @Override
    public PlanElement build(Field field, Restriction restriction) {
      DBObject configuration = restriction.getConfig();
      String[] fields = (String[]) configuration.get("fields");
      return new UniqueFieldsRestriction(fields);
    }

  }

  private static class CountBuffer extends BaseOperation implements Buffer {

    CountBuffer() {
      super(Fields.ARGS);
    }

    @Override
    public void prepare(FlowProcess flowProcess, OperationCall operationCall) {
      // TODO Auto-generated method stub
      super.prepare(flowProcess, operationCall);
    }

    @Override
    public void operate(FlowProcess flowProcess, BufferCall bufferCall) {
      int count = 0;
      Iterator<TupleEntry> i = bufferCall.getArgumentsIterator();
      while(i.hasNext()) {
        TupleEntry tupleEntry = i.next();
        if(count > 0) {
          ValidationFields.state(tupleEntry).reportError(500, "not unique");
        }
        count++;
        bufferCall.getOutputCollector().add(tupleEntry.getTupleCopy());
      }
    }
  }

  private static class CountIsOne extends BaseOperation implements Function {

    private CountIsOne() {
      super(2, ValidationFields.STATE_FIELD);
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      int count = functionCall.getArguments().getInteger("count");
      if(count > 1) {
        TupleState state = ValidationFields.state(functionCall.getArguments());
        state.reportError(500, count);
        functionCall.getOutputCollector().add(new Tuple(state));
      } else {

      }
    }
  }

}
