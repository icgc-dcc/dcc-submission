package org.icgc.dcc.validation;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.Restriction;

public interface RestrictionType {

  public String getType();

  public boolean builds(String type);

  public RestrictionTypeSchema getSchema();

  public PipeExtender build(Field field, Restriction restriction);

}
