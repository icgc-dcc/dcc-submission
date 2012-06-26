package org.icgc.dcc.validation.restriction;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.ValueType;
import org.icgc.dcc.validation.cascading.ValidationFields;
import org.icgc.dcc.validation.plan.InternalPlanElement;
import org.icgc.dcc.validation.plan.PlanPhase;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

public class ValueTypePlanElement implements InternalPlanElement {

  private final Field field;

  private final ValueType type;

  public ValueTypePlanElement(Field field) {
    this.field = field;
    this.type = field.getValueType();
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
    return new Each(pipe, new ValidationFields(field.getName()), new ValueTypeFunction(), Fields.REPLACE);
  }

  private final class ValueTypeFunction extends BaseOperation implements Function {

    ValueTypeFunction() {
      super(2, Fields.ARGS);
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      String value = functionCall.getArguments().getString(0);
      try {
        Object parsedValue = parse(value);
        functionCall.getOutputCollector().add(
            new Tuple(parsedValue, ValidationFields.state(functionCall.getArguments())));
      } catch(IllegalArgumentException e) {
        ValidationFields.state(functionCall.getArguments()).reportError(500, value);
        functionCall.getOutputCollector().add(functionCall.getArguments());
      }
    }

    private Object parse(String value) {
      switch(type) {
      case DATETIME:
        // TODO: parse datetime
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
