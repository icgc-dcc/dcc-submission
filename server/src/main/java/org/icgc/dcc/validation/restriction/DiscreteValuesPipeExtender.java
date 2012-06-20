package org.icgc.dcc.validation.restriction;

import java.util.Arrays;
import java.util.Set;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.validation.PipeExtender;
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

import com.google.common.collect.ImmutableSet;

public class DiscreteValuesPipeExtender implements PipeExtender {

  private static final String NAME = "in";

  private final String field;

  private final String[] values;

  private DiscreteValuesPipeExtender(String field, String[] values) {
    this.field = field;
    this.values = values;
  }

  @Override
  public String describe() {
    return String.format("in[%s]", Arrays.toString(values));
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new InValuesFunction(values), Fields.REPLACE);
  }

  public static class Type implements RestrictionType {

    private final RestrictionTypeSchema schema = new RestrictionTypeSchema(//
        new FieldRestrictionParameter("values", ParameterType.TEXT, "list of allowable values (e.g.: 1,2,3)", true));

    @Override
    public String getType() {
      return NAME;
    }

    @Override
    public boolean builds(String name) {
      return getType().equals(name);
    }

    @Override
    public RestrictionTypeSchema getSchema() {
      return schema;
    }

    @Override
    public PipeExtender build(Field field, Restriction restriction) {
      String[] values = (String[]) restriction.getConfig().get("values");
      return new DiscreteValuesPipeExtender(field.getName(), values);
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
