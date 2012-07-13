package org.icgc.dcc.validation.restriction;

import java.util.Arrays;
import java.util.Set;

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

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

public class DiscreteValuesRestriction implements InternalPlanElement {

  private static final String NAME = "in";

  private static final String DESCRIPTION = "list of allowable values (e.g.: 1,2,3)";

  private static final String PARAM = "values";

  private final String field;

  private final String[] values;

  protected DiscreteValuesRestriction(String field, String[] values) {
    this.field = field;
    this.values = values;
  }

  @Override
  public String describe() {
    return String.format("%s[%s:%s]", NAME, field, Arrays.toString(values));
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new InValuesFunction(values), Fields.REPLACE);
  }

  public static class Type implements RestrictionType {

    private final RestrictionTypeSchema schema = new RestrictionTypeSchema(//
        new FieldRestrictionParameter(PARAM, ParameterType.TEXT, DESCRIPTION, true));

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
      String valuesString = (String) restriction.getConfig().get(PARAM);// expects comma-separated like: "male,female"
      Iterable<String> split = Splitter.on(Restriction.CONFIG_VALUE_SEPARATOR).split(valuesString);
      String[] values = Iterables.toArray(split, String.class);
      return new DiscreteValuesRestriction(field.getName(), values);
    }
  }

  @SuppressWarnings("rawtypes")
  public static class InValuesFunction extends BaseOperation implements Function {

    private final Set<String> values;

    protected InValuesFunction(String[] values) {
      super(2, Fields.ARGS);
      this.values = ImmutableSet.copyOf(values);
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      TupleEntry tupleEntry = functionCall.getArguments();
      String value = tupleEntry.getString(0);
      if(value != null && values.contains(value) == false) {
        Object fieldName = tupleEntry.getFields().get(0);
        ValidationFields.state(tupleEntry).reportError(ValidationErrorCode.DISCRETE_VALUES_ERROR, value, fieldName,
            values);
      }
      functionCall.getOutputCollector().add(tupleEntry.getTupleCopy());
    }

  }
}
