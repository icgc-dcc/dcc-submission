package org.icgc.dcc.validation;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.validation.FieldRestrictionSchema.FieldRestrictionParameter;
import org.icgc.dcc.validation.FieldRestrictionSchema.Type;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;

import com.mongodb.DBObject;

public class RangeFieldRestriction implements FieldRestriction, PipeExtender {

  private static final String NAME = "range";

  private final String field;

  private final Number min;

  private final Number max;

  private RangeFieldRestriction(String field, Number min, Number max) {
    this.field = field;
    this.min = min;
    this.max = max;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getLabel() {
    return String.format("range[%d-%d]", min, max);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new RangeFunction(min, max), Fields.REPLACE);
  }

  public static class Factory implements FieldRestrictionFactory {

    private final FieldRestrictionSchema schema = new FieldRestrictionSchema(//
        new FieldRestrictionParameter("min", Type.NUMBER, "minimum value (inclusive)"), //
        new FieldRestrictionParameter("max", Type.NUMBER, "maximu value (exclusive)"));

    @Override
    public String getType() {
      return NAME;
    }

    @Override
    public boolean builds(String name) {
      return getType().equals(name);
    }

    @Override
    public FieldRestrictionSchema getSchema() {
      return schema;
    }

    @Override
    public RangeFieldRestriction build(Field field, DBObject configuration) {
      Number min = (Number) configuration.get("min");
      Number max = (Number) configuration.get("max");
      return new RangeFieldRestriction(field.getName(), min, max);
    }

  }

  public class RangeFunction extends BaseOperation implements Function {

    private final Number min;

    private final Number max;

    private RangeFunction(Number min, Number max) {
      super(2, Fields.ARGS);
      this.min = min;
      this.max = max;
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      Object value = functionCall.getArguments().getObject(0);
      // TODO: range check
      functionCall.getOutputCollector().add(functionCall.getArguments());
    }

  }
}
