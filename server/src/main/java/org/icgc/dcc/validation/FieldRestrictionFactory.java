package org.icgc.dcc.validation;

import org.icgc.dcc.model.dictionary.Field;

import com.mongodb.DBObject;

public interface FieldRestrictionFactory {

  public boolean builds(String name);

  public FieldRestriction build(Field field, DBObject configuration);

}
