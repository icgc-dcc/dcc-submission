package org.icgc.dcc.validation.restriction;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.validation.ErrorCodeRegistry;
import org.icgc.dcc.validation.FlowType;
import org.icgc.dcc.validation.InternalPlanElement;
import org.icgc.dcc.validation.PlanElement;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.RestrictionTypeSchema;
import org.icgc.dcc.validation.cascading.ValidationFields;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;

public class RequiredRestriction implements InternalPlanElement {

  private static final String NAME = "required";// TODO: create enum for valid Restriction types?

  private static final int CODE = 503;

  private static final String MESSAGE = "value missing for required field: %s";

  private final String field;

  private RequiredRestriction(String field) {
    this.field = field;
  }

  @Override
  public String describe() {
    return String.format("%s[%s]", NAME, field);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new SpecifiedFunction(), Fields.REPLACE);
  }

  public static class Type implements RestrictionType {

    private final RestrictionTypeSchema schema = new RestrictionTypeSchema();

    public Type() {
      ErrorCodeRegistry.get().register(CODE, MESSAGE);
    }

    @Override
    public String getType() {
      return NAME;
    }

    @Override
    public FlowType flow() {
      return FlowType.INTERNAL;
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
    public PlanElement build(Field field, Restriction restriction) {
      return new RequiredRestriction(field.getName());
    }

  }

  @SuppressWarnings("rawtypes")
  public class SpecifiedFunction extends BaseOperation implements Function {

    private SpecifiedFunction() {
      super(2, Fields.ARGS);
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      String value = functionCall.getArguments().getString(0);
      if(value == null || value.isEmpty()) {
        Object fieldName = functionCall.getArguments().getFields().get(0);
        ValidationFields.state(functionCall.getArguments()).reportError(CODE, value, fieldName);
      }
      functionCall.getOutputCollector().add(functionCall.getArguments());
    }

  }
}
