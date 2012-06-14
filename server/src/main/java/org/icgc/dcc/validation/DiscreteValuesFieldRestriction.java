package org.icgc.dcc.validation;

import java.util.Arrays;
import java.util.Set;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.validation.CascadeBuilder.PipeExtender;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;

import com.google.common.collect.ImmutableSet;
import com.mongodb.DBObject;

public class DiscreteValuesFieldRestriction implements FieldRestriction, PipeExtender {

  private static final String NAME = "in";

  private final String field;

  private final String[] values;

  private DiscreteValuesFieldRestriction(String field, String[] values) {
    this.field = field;
    this.values = values;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getLabel() {
    return String.format("in[%s]", Arrays.toString(values));
  }

  @Override
  public void visitCascade(FileSchema schema, Field field, CascadeBuilder builder) {

  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new InValuesFunction(values), Fields.REPLACE);
  }

  public static class Factory implements FieldRestrictionFactory {

    @Override
    public boolean builds(String name) {
      return NAME.equals(name);
    }

    @Override
    public DiscreteValuesFieldRestriction build(Field field, DBObject configuration) {
      String[] values = (String[]) configuration.get("values");
      return new DiscreteValuesFieldRestriction(field.getName(), values);
    }

  }

  public class InValuesFunction extends BaseOperation implements Function {

    private final Set<String> values;

    private InValuesFunction(String[] values) {
      super(2, Fields.ARGS);
      this.values = ImmutableSet.copyOf(values);
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      String value = functionCall.getArguments().getString(0);
      if(values.contains(value) == false) {
        ValidationFields.state(functionCall.getArguments()).reportError(500, value, values);
      }
      functionCall.getOutputCollector().add(functionCall.getArguments());
    }

  }
}
