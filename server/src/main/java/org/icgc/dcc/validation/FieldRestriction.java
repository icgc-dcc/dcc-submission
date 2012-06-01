package org.icgc.dcc.validation;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;

public interface FieldRestriction {

  public String getName();

  public String getLabel();

  public void visitCascade(FileSchema schema, Field field, CascadeBuilder builder);

}
