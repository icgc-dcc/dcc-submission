package org.icgc.dcc.model.dictionary;

import java.util.List;

public class FileSchema {

  enum Role {
    SUBMISSION, SYSTEM
  }

  public String name;

  public String label;

  public String pattern;

  public Role role;

  public List<Field> fields;

}
