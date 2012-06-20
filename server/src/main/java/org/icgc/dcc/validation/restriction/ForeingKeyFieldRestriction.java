package org.icgc.dcc.validation.restriction;

import static com.google.common.base.Preconditions.checkState;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.RestrictionTypeSchema;
import org.icgc.dcc.validation.cascading.ValidationFields;
import org.icgc.dcc.validation.plan.ExternalIntegrityPlanElement;
import org.icgc.dcc.validation.plan.FileSchemaPlan;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.pipe.CoGroup;
import cascading.pipe.Every;
import cascading.pipe.Pipe;
import cascading.pipe.joiner.LeftJoin;
import cascading.tuple.Fields;

import com.mongodb.DBObject;

public class ForeingKeyFieldRestriction implements ExternalIntegrityPlanElement {

  private static final String NAME = "foreign-key";

  private final String lhsField;

  private final String schema;

  private final String field;

  private ForeingKeyFieldRestriction(String lhsField, String schema, String field) {
    this.lhsField = lhsField;
    this.schema = schema;
    this.field = field;
  }

  @Override
  public String describe() {
    return String.format("fk[%s:%s]", schema, field);
  }

  @Override
  public String[] lhsFields() {
    return new String[] { lhsField };
  }

  @Override
  public String rhs() {
    return schema;
  }

  @Override
  public String[] rhsFields() {
    return new String[] { field };
  }

  @Override
  public Pipe join(Pipe lhs, Pipe rhs) {
    String joinedFieldName = schema + "$" + field;
    Pipe pipe =
        new CoGroup(lhs, new Fields(lhsField), rhs, new Fields(field), new Fields(lhsField,
            ValidationFields.STATE_FIELD_NAME, joinedFieldName), new LeftJoin());
    pipe = new Every(pipe, new ValidationFields(lhsField, joinedFieldName), new NoNullBuffer(), Fields.RESULTS);
    return pipe;
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
    public void apply(FileSchemaPlan plan, Field field, Restriction restriction) {
      DBObject configuration = restriction.getConfig();
      checkState(configuration.containsField("schema"));
      checkState(configuration.containsField("field"));
      plan.apply(new ForeingKeyFieldRestriction(field.getName(), configuration.get("schema").toString(), configuration
          .get("field").toString()));
    }

  }

  private static class NoNullBuffer extends BaseOperation implements Buffer {

    @Override
    public void operate(FlowProcess flowProcess, BufferCall bufferCall) {

    }
  }

}
