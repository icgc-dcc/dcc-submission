package org.icgc.dcc.validation.restriction;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.validation.ErrorCodeRegistry;
import org.icgc.dcc.validation.FlowType;
import org.icgc.dcc.validation.InternalPlanElement;
import org.icgc.dcc.validation.PlanElement;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.RestrictionTypeSchema;
import org.icgc.dcc.validation.RestrictionTypeSchema.FieldRestrictionParameter;
import org.icgc.dcc.validation.RestrictionTypeSchema.ParameterType;
import org.icgc.dcc.validation.cascading.ValidationFields;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;

import com.mongodb.DBObject;

public class RangeFieldRestriction implements InternalPlanElement {

  private static final String NAME = "range";

  private static final int RANGE_CODE = 501;

  private static final String RANGE_MESSAGE =
      "number %d is out of range for field %s. Expected value between %d and %d";

  private static final int TYPE_CODE = 502;

  private static final String TYPE_MESSAGE = "%s is not a number for field %s. Expected a number";

  private static final String MIN = "min";

  private static final String MAX = "max";

  private final String field;

  private final Number min;

  private final Number max;

  protected RangeFieldRestriction(String field, Number min, Number max) {
    this.field = field;
    this.min = min;
    this.max = max;
  }

  @Override
  public String describe() {
    return String.format("%s[%d-%d]", NAME, min, max);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new RangeFunction(min, max), Fields.REPLACE);
  }

  public static class Type implements RestrictionType {

    private final RestrictionTypeSchema schema = new RestrictionTypeSchema(//
        new FieldRestrictionParameter(MIN, ParameterType.NUMBER, "minimum value (inclusive)"), //
        new FieldRestrictionParameter(MAX, ParameterType.NUMBER, "maximum value (inclusive)"));

    public Type() {
      ErrorCodeRegistry.get().register(RANGE_CODE, RANGE_MESSAGE);
      ErrorCodeRegistry.get().register(TYPE_CODE, TYPE_MESSAGE);
    }

    @Override
    public String getType() {
      return NAME;
    }

    @Override
    public boolean builds(String name) {
      return getType().equals(name);
    }

    @Override
    public FlowType flow() {
      return FlowType.INTERNAL;
    }

    @Override
    public RestrictionTypeSchema getSchema() {
      return schema;
    }

    @Override
    public PlanElement build(Field field, Restriction restriction) {
      DBObject configuration = restriction.getConfig();
      Number min = (Number) configuration.get(MIN);
      Number max = (Number) configuration.get(MAX);
      return new RangeFieldRestriction(field.getName(), min, max);
    }

  }

  @SuppressWarnings("rawtypes")
  public static class RangeFunction extends BaseOperation implements Function {

    private final Number min;

    private final Number max;

    protected RangeFunction(Number min, Number max) {
      super(2, Fields.ARGS);
      this.min = min;
      this.max = max;
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      Object value = functionCall.getArguments().getObject(0);

      Object fieldName = functionCall.getArguments().getFields().get(0);

      if(value instanceof Number) {
        Number num = (Number) value;
        if(num.longValue() < this.min.longValue() || num.longValue() > this.max.longValue()) {
          ValidationFields.state(functionCall.getArguments()).reportError(RANGE_CODE, num.longValue(), fieldName,
              this.min.longValue(), this.max.longValue());
        }
      } else {
        ValidationFields.state(functionCall.getArguments()).reportError(TYPE_CODE, value.toString(), fieldName);
      }
      functionCall.getOutputCollector().add(functionCall.getArguments());
    }

  }
}
