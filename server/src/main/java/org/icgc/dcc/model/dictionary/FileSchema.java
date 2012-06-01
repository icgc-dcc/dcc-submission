package org.icgc.dcc.model.dictionary;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

public class FileSchema {

  enum Role {
    SUBMISSION, SYSTEM
  }

  public String name;

  public String label;

  public String pattern;

  public Role role;

  public List<Field> fields;

  public Optional<Field> field(final String name) {
    return Iterables.tryFind(fields, new Predicate<Field>() {

      @Override
      public boolean apply(Field input) {
        return input.name.equals(name);
      }
    });
  }

  public Iterable<String> fieldNames() {
    return Iterables.transform(fields, new Function<Field, String>() {

      @Override
      public String apply(Field input) {
        return input.name;
      }
    });
  }
}
