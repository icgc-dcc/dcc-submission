package org.icgc.dcc.validation;

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.Restriction;

public interface RestrictionType {

  public String getType();

  public boolean builds(String type);

  public FlowType flow();

  public RestrictionTypeSchema getSchema();

  public PlanElement build(Field field, Restriction restriction);

}
