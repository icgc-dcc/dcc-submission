package org.icgc.dcc.validation.function;

import static com.google.common.base.Preconditions.checkArgument;

import org.icgc.dcc.model.dictionary.FileSchema;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

import com.google.common.collect.Iterables;

public class FieldCountFunction extends BaseOperation implements Function {

  private final FileSchema schema;

  private final Fields schemaFields;

  public FieldCountFunction(FileSchema schema) {
    super(Fields.ALL);
    checkArgument(schema != null);
    this.schema = schema;
    this.schemaFields = new Fields(Iterables.toArray(schema.fieldNames(), String.class));
  }

  @Override
  public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
    Tuple argsCopy = functionCall.getArguments().getTupleCopy();
    if(functionCall.getArguments().getFields().contains(schemaFields) == false) {
      System.err.print("count is wrong");
      // TODO: report error - invalid # of fields
    }
    functionCall.getOutputCollector().add(argsCopy);
  }

}
