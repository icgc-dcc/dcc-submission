package org.icgc.dcc.dictionary.visitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.icgc.dcc.dictionary.model.FileSchema;

import com.google.common.collect.Sets;

public class DictionaryConsistencyVisitor extends BaseDictionaryVisitor {
  private final List<String> errors = new ArrayList<String>();

  @Override
  public void visit(FileSchema schema) {
    Set<String> fieldNames = Sets.newHashSet(schema.fieldNames());
    for(String uniqueField : schema.getUniqueFields()) {
      if(fieldNames.contains(uniqueField) == false) {
        errors.add("Specified uniqueField does not exist in field list: " + uniqueField);
      }
    }
  }

  public boolean hasErrors() {
    return errors.isEmpty() == false;
  }

  public List<String> getErrors() {
    return errors;
  }
}
