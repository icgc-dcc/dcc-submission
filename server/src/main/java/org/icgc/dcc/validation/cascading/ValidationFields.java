package org.icgc.dcc.validation.cascading;

import java.beans.ConstructorProperties;

import org.icgc.dcc.loader.CascadingUtils;

import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;

/**
 * An extension of {@code Fields} that always includes the {@code TupleState} field.
 */
public class ValidationFields extends Fields {

  public static final String STATE_FIELD_NAME = "_state";

  public static final String OFFSET_FIELD_NAME = "offset";

  public static final Fields STATE_FIELD = new Fields(STATE_FIELD_NAME);

  /**
   * Extract the {@code TupleState} field from a {@cude TupleEntry}.
   */
  public static TupleState state(TupleEntry te) {
    return (TupleState) te.getObject(STATE_FIELD_NAME);
  }

  @SuppressWarnings("rawtypes")
  @ConstructorProperties({ "fields" })
  public ValidationFields(Comparable... fields) {
    super(CascadingUtils.concat(fields, STATE_FIELD_NAME));
  }

}
