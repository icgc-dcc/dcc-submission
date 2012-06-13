package org.icgc.dcc.validation.function;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.model.dictionary.ValueType;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

import com.google.common.base.Optional;

public final class FieldsValueTypeFunction extends BaseOperation implements Function {

  private final FileSchema schema;

  public FieldsValueTypeFunction(FileSchema schema) {
    super(Fields.ARGS);
    this.schema = schema;
  }

  @Override
  public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
    Tuple result = new Tuple();
    // Iterate in arguments order so we add results in the same order
    for(Comparable argument : functionCall.getArguments().getFields()) {
      Optional<Field> maybeField = schema.field(argument.toString());
      if(maybeField.isPresent()) {
        String value = functionCall.getArguments().getString(argument);
        try {
          Object parsedValue = parse(maybeField.get().getValueType(), value);
          result.add(parsedValue);
        } catch(IllegalArgumentException e) {
          result.add(null);
          // TODO: report error
        }
      } else {
        result.add(functionCall.getArguments().getObject(argument));
      }
    }
    functionCall.getOutputCollector().add(result);
  }

  private Object parse(ValueType valueType, String value) {
    switch(valueType) {
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