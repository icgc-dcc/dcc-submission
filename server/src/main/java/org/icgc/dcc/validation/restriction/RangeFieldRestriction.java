package org.icgc.dcc.validation.restriction;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.RestrictionTypeSchema;
import org.icgc.dcc.validation.RestrictionTypeSchema.FieldRestrictionParameter;
import org.icgc.dcc.validation.RestrictionTypeSchema.ParameterType;
import org.icgc.dcc.validation.cascading.ValidationFields;
import org.icgc.dcc.validation.plan.FlowType;
import org.icgc.dcc.validation.plan.InternalPlanElement;
import org.icgc.dcc.validation.plan.PlanElement;

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

  private final String field;

  private final Number min;

  private final Number max;

  private RangeFieldRestriction(String field, Number min, Number max) {
    this.field = field;
    this.min = min;
    this.max = max;
  }

  @Override
  public String describe() {
    return String.format("range[%d-%d]", min, max);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new RangeFunction(min, max), Fields.REPLACE);
  }

  public static class Type implements RestrictionType {

    private final RestrictionTypeSchema schema = new RestrictionTypeSchema(//
        new FieldRestrictionParameter("min", ParameterType.NUMBER, "minimum value (inclusive)"), //
        new FieldRestrictionParameter("max", ParameterType.NUMBER, "maximum value (inclusive)"));

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
