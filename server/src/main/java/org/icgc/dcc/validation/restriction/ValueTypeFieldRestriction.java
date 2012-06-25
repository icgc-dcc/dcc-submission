package org.icgc.dcc.validation.restriction;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.model.dictionary.ValueType;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.RestrictionTypeSchema;
import org.icgc.dcc.validation.cascading.ValidationFields;
import org.icgc.dcc.validation.plan.InternalIntegrityPlanElement;
import org.icgc.dcc.validation.plan.PlanElement;
import org.icgc.dcc.validation.plan.PlanPhase;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

public class ValueTypeFieldRestriction implements InternalIntegrityPlanElement {

  private static final String NAME = "value-type";

  private final String field;

  private final ValueType type;

  private ValueTypeFieldRestriction(String field, ValueType type) {
    this.field = field;
    this.type = type;
  }

  @Override
  public String describe() {
    return String.format("valueType[%s]", type);
  }

  @Override
  public PlanPhase phase() {
    return PlanPhase.INTERNAL;
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new ValueTypeFunction(), Fields.REPLACE);
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
      return new ValueTypeFieldRestriction(field.getName(), field.getValueType());
    }
  }

  private final class ValueTypeFunction extends BaseOperation implements Function {

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      String value = functionCall.getArguments().getString(0);
      try {
        Object parsedValue = parse(value);
        functionCall.getOutputCollector().add(new Tuple(parsedValue));
      } catch(IllegalArgumentException e) {
        ValidationFields.state(functionCall.getArguments()).reportError(500, value);
        functionCall.getOutputCollector().add(new Tuple((Object) null));
      }
    }

    private Object parse(String value) {
      switch(type) {
      case DATETIME:
        break;
      case DECIMAL:
        return Double.valueOf(value);
      case INTEGER:
        return Long.valueOf(value);
      case TEXT:
        return value;
      }
      return null;
    }

  }
}
