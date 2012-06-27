package org.icgc.dcc.validation;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.validation.plan.FlowType;
import org.icgc.dcc.validation.plan.PlanElement;

public interface RestrictionType {

  public String getType();

  public boolean builds(String type);

  public FlowType flow();

  public RestrictionTypeSchema getSchema();

  public PlanElement build(Field field, Restriction restriction);

}
