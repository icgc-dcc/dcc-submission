package org.icgc.dcc.validation.restriction;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.ValueType;
import org.icgc.dcc.validation.FieldRestriction;
import org.icgc.dcc.validation.FieldRestrictionType;
import org.icgc.dcc.validation.FieldRestrictionTypeSchema;
import org.icgc.dcc.validation.ValidationFields;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

import com.mongodb.DBObject;

public class ValueTypeFieldRestriction implements FieldRestriction {

  private static final String NAME = "value-type";

  private final String field;

  private final ValueType type;

  private ValueTypeFieldRestriction(String field, ValueType type) {
    this.field = field;
    this.type = type;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getLabel() {
    return String.format("valueType[%s]", type);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new ValueTypeFunction(), Fields.REPLACE);
  }

  public static class Type implements FieldRestrictionType {

    @Override
    public String getType() {
      return NAME;
    }

    @Override
    public FieldRestrictionTypeSchema getSchema() {
      return null;
    }

    @Override
    public boolean builds(String name) {
      return NAME.equals(name);
    }

    @Override
    public FieldRestriction build(Field field, DBObject configuration) {
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
