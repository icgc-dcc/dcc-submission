package org.icgc.dcc.validation;

import org.icgc.dcc.model.dictionary.Field;

import com.mongodb.DBObject;

public interface FieldRestrictionFactory {

  public String getType();

  public boolean builds(String name);

  public FieldRestrictionSchema getSchema();

  public FieldRestriction build(Field field, DBObject configuration);

}
