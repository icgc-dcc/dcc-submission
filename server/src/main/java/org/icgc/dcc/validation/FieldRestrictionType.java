package org.icgc.dcc.validation;

import org.icgc.dcc.model.dictionary.Field;

import com.mongodb.DBObject;

public interface FieldRestrictionType {

  public String getType();

  public boolean builds(String type);

  public FieldRestrictionTypeSchema getSchema();

  public FieldRestriction build(Field field, DBObject configuration);

}
