package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkState;

import com.mongodb.DBObject;

public class ForeingKeyFieldRestriction implements FieldRestriction {

  private static final String TYPE = "foreign-key";

  public static class Factory implements FieldRestrictionFactory {

    @Override
    public boolean buildsType(String type) {
      return TYPE.equals(type);
    }

    @Override
    public FieldRestriction build(DBObject configuration) {
      checkState(configuration.containsField("schema"));
      checkState(configuration.containsField("field"));

      return new ForeingKeyFieldRestriction(configuration.get("schema").toString(), configuration.get("field")
          .toString());
    }

  }

  private final String schema;

  private final String field;

  ForeingKeyFieldRestriction(String schema, String field) {
    this.schema = schema;
    this.field = field;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String getLabel() {
    return String.format("fk[%s:%s]", schema, field);
  }

}
