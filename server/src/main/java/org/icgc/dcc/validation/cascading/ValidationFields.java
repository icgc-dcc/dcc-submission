package org.icgc.dcc.validation.cascading;

import java.beans.ConstructorProperties;
import java.util.Arrays;

import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;

public class ValidationFields extends Fields {

  public static final String STATE_FIELD_NAME = "_state";

  public static final Fields STATE_FIELD = new Fields(STATE_FIELD_NAME);

  public static TupleState state(TupleEntry te) {
    return (TupleState) te.getObject(STATE_FIELD_NAME);
  }

  @ConstructorProperties({ "fields" })
  public ValidationFields(Comparable... fields) {
    super(concat(fields, STATE_FIELD_NAME));
  }

  private static Comparable[] concat(Comparable[] fields, Comparable... extra) {
    Comparable[] concatenated = Arrays.copyOf(fields, fields.length + extra.length);
    for(int i = 0; i < extra.length; i++) {
      concatenated[i + fields.length] = extra[i];
    }
    return concatenated;
  }

}
