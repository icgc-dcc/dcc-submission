package org.icgc.dcc.validation;

import com.mongodb.DBObject;

public interface FieldRestrictionFactory {

  public boolean buildsType(String type);

  public FieldRestriction build(DBObject configuration);

}
