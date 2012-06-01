package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkState;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;

import com.mongodb.DBObject;

public class ForeingKeyFieldRestriction implements FieldRestriction {

  private static final String NAME = "foreign-key";

  private final String schema;

  private final String field;

  private ForeingKeyFieldRestriction(String schema, String field) {
    this.schema = schema;
    this.field = field;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getLabel() {
    return String.format("fk[%s:%s]", schema, field);
  }

  @Override
  public void visitCascade(FileSchema schema, Field field, CascadeBuilder builder) {
    // TODO Auto-generated method stub

  }

  public static class Factory implements FieldRestrictionFactory {

    @Override
    public boolean builds(String name) {
      return NAME.equals(name);
    }

    @Override
    public FieldRestriction build(Field field, DBObject configuration) {
      checkState(configuration.containsField("schema"));
      checkState(configuration.containsField("field"));

      return new ForeingKeyFieldRestriction(configuration.get("schema").toString(), configuration.get("field")
          .toString());
    }

  }

}
