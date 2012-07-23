package org.icgc.dcc.validation.restriction;

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.Restriction;
import org.icgc.dcc.validation.FlowType;
import org.icgc.dcc.validation.InternalPlanElement;
import org.icgc.dcc.validation.PlanElement;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.RestrictionTypeSchema;
import org.icgc.dcc.validation.RestrictionTypeSchema.FieldRestrictionParameter;
import org.icgc.dcc.validation.RestrictionTypeSchema.ParameterType;
import org.icgc.dcc.validation.ValidationErrorCode;
import org.icgc.dcc.validation.cascading.ValidationFields;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;

import com.mongodb.DBObject;

public class RangeFieldRestriction implements InternalPlanElement {

  public static final String NAME = "range";

  public static final String MIN = "min";

  public static final String MAX = "max";

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
      TupleEntry tupleEntry = functionCall.getArguments();
      Object value = tupleEntry.getObject(0);

      Object fieldName = tupleEntry.getFields().get(0);

      if(value instanceof Number) {
        Number num = (Number) value;
        if(num.longValue() < this.min.longValue() || num.longValue() > this.max.longValue()) {
          ValidationFields.state(tupleEntry).reportError(ValidationErrorCode.OUT_OF_RANGE_ERROR, num.longValue(),
              fieldName, this.min.longValue(), this.max.longValue());
        }
      } else if(value != null) {
        ValidationFields.state(tupleEntry).reportError(ValidationErrorCode.NOT_A_NUMBER_ERROR, value.toString(),
            fieldName);
      }
      functionCall.getOutputCollector().add(tupleEntry.getTupleCopy());
    }

  }
}
