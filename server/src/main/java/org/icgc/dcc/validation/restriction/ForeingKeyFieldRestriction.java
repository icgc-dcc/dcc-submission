package org.icgc.dcc.validation.restriction;

import static com.google.common.base.Preconditions.checkState;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.validation.PipeExtender;
import org.icgc.dcc.validation.PipeJoiner;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.RestrictionTypeSchema;
import org.icgc.dcc.validation.cascading.ValidationFields;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.pipe.CoGroup;
import cascading.pipe.Every;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.Retain;
import cascading.pipe.joiner.LeftJoin;
import cascading.tuple.Fields;

import com.mongodb.DBObject;

public class ForeingKeyFieldRestriction implements PipeJoiner {

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
  public Pipe join(Pipe lhs, Pipe rhs) {
    Pipe lhsTrimmed = new Retain(lhs, new ValidationFields(lhsField));
    Pipe rhsTrimmed = new Retain(rhs, new Fields(field));
    Pipe pipe =
        new CoGroup(lhsTrimmed, new Fields(lhsField), rhsTrimmed, new Fields(field), new Fields(lhsField,
            ValidationFields.STATE_FIELD_NAME, schema + "$" + field), new LeftJoin());
    pipe = new Every(pipe, new ValidationFields(lhsField, field), new NoNullBuffer(), Fields.RESULTS);
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
    public PipeExtender build(Field field, Restriction restriction) {
      DBObject configuration = restriction.getConfig();
      checkState(configuration.containsField("schema"));
      checkState(configuration.containsField("field"));
      new ForeingKeyFieldRestriction(field.getName(), configuration.get("schema").toString(), configuration
          .get("field").toString());
      return null;
    }

  }

  private static class NoNullBuffer extends BaseOperation implements Buffer {

    @Override
    public void operate(FlowProcess flowProcess, BufferCall bufferCall) {

    }
  }

}
