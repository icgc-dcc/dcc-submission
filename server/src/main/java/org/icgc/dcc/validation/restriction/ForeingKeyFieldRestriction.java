package org.icgc.dcc.validation.restriction;

import static com.google.common.base.Preconditions.checkState;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.validation.FieldRestrictionTypeSchema;
import org.icgc.dcc.validation.PipeExtender;
import org.icgc.dcc.validation.RestrictionType;

import cascading.pipe.Pipe;

import com.mongodb.DBObject;

public class ForeingKeyFieldRestriction implements PipeExtender {

  private static final String NAME = "foreign-key";

  private final String schema;

  private final String field;

  private ForeingKeyFieldRestriction(String schema, String field) {
    this.schema = schema;
    this.field = field;
  }

  @Override
  public String describe() {
    return String.format("fk[%s:%s]", schema, field);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return null;
  }

  public static class Type implements RestrictionType {

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
    public PipeExtender build(Field field, Restriction restriction) {
      DBObject configuration = restriction.getConfig();
      checkState(configuration.containsField("schema"));
      checkState(configuration.containsField("field"));

      return new ForeingKeyFieldRestriction(configuration.get("schema").toString(), configuration.get("field")
          .toString());
    }

  }

}
