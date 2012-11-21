package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.icgc.dcc.dictionary.model.ValueType;
import org.icgc.dcc.validation.cascading.TupleState.TupleError;

import com.google.common.collect.ImmutableMap;

public enum ValidationErrorCode { // TODO: DCC-505 to fix the message (currently not used for anything)

  /**
   * Number of columns does not match that of header.
   */
  STRUCTURALLY_INVALID_ROW_ERROR("structurally invalid row: %s columns against %s declared in the header (row will be ignored by the rest of validation)", true) {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkArgument(params != null);
      checkArgument(params.length == 1);
      checkArgument(params[0] instanceof Integer);
      return ImmutableMap.of(ErrorParameterKey.EXPECTED, params[0]);
    }
  }, //
  /**
   * No matching value(s) for referencED field(s).
   * <p>
   * Example: for specimen report if say specimen.donor_id holds value "my_donor_X" but donor.donor_id does not
   * <p>
   * Not to be confused with its file counterpart {@code RELATION_FILE_ERROR}
   */
  RELATION_VALUE_ERROR("invalid value(s) (%s) for field(s) %s.%s. Expected to match value(s) in: %s.%s") {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkArgument(params != null);
      checkArgument(params.length == 2);
      checkArgument(params[0] instanceof String);
      checkArgument(params[1] instanceof List);
      return ImmutableMap.of(ErrorParameterKey.SCHEMA, params[0], ErrorParameterKey.FIELDS, params[1]);
    }
  }, //
  /**
   * No matching value(s) for referencING field(s) (only applicable if relation is set to bidirectional).
   * <p>
   * Example: e.g,. for specimen report: if specimen.donor_id does not hold value "my_donor_B" but donor.donor_id does
   * (would not be a problem with family.donor_id as the relation from family to donor is not bidirectional with donor,
   * unlike the one from specimen to donor)
   * <p>
   * Not quite the value-counterpart to {@code REVERSE_RELATION_FILE_ERROR}
   */
  RELATION_PARENT_VALUE_ERROR("no corresponding values in %s.%s for value(s) %s in %s.%s") {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkArgument(params != null);
      checkArgument(params.length == 2);
      checkArgument(params[0] instanceof String);
      checkArgument(params[1] instanceof List);
      return ImmutableMap.of(ErrorParameterKey.SCHEMA, params[0], ErrorParameterKey.FIELDS, params[1]);
    }
  }, //
  /**
   * Duplicate values in unique field(s).
   */
  UNIQUE_VALUE_ERROR("invalid set of values (%s) for fields %s. Expected to be unique") {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  }, //
  /**
   * Invalid value type (i.e. a string where an integer is expected).
   */
  VALUE_TYPE_ERROR("invalid value (%s) for field %s. Expected type is: %s") {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkArgument(params != null);
      checkArgument(params.length == 1);
      checkArgument(params[0] instanceof ValueType);
      return ImmutableMap.of(ErrorParameterKey.EXPECTED, params[0]);
    }
  }, //
  /**
   * Value out for (inclusive) range.
   */
  OUT_OF_RANGE_ERROR("number %d is out of range for field %s. Expected value between %d and %d") {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkArgument(params != null);
      checkArgument(params.length == 2);
      checkArgument(params[0] instanceof Long);
      checkArgument(params[1] instanceof Long);

      return ImmutableMap.of(ErrorParameterKey.MIN, params[0], ErrorParameterKey.MAX, params[1]);
    }
  }, //
  /**
   * Range value is not numerical.
   */
  NOT_A_NUMBER_ERROR("%s is not a number for field %s. Expected a number") {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  }, //
  /**
   * Missing required value.
   */
  MISSING_VALUE_ERROR("value missing for required field: %s") {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  }, //
  /**
   * Values not in code list (as codes)
   */
  CODELIST_ERROR("invalid value %s for field %s. Expected code or value from CodeList %s") {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  }, //
  /**
   * Values not in set of discrete values.
   */
  DISCRETE_VALUES_ERROR("invalid value %s for field %s. Expected one of the following values: %s") {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkArgument(params != null);
      checkArgument(params.length == 1);
      checkArgument(params[0] instanceof Set);
      return ImmutableMap.of(ErrorParameterKey.EXPECTED, params[0]);
    }
  }, //
  /**
   * More than one file matches the schema pattern.
   */
  TOO_MANY_FILES_ERROR("more than one file matches the schema pattern") {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkArgument(params != null);
      checkArgument(params.length == 2);
      checkArgument(params[0] instanceof String);
      checkArgument(params[1] instanceof List);
      return ImmutableMap.of(ErrorParameterKey.SCHEMA, params[0], ErrorParameterKey.FILES, params[1]);
    }
  }, //
  /**
   * No matching file for referencED schema.
   * <p>
   * Example: for specimen, if donor is missing
   * <p>
   * Not to be confused with its value counterpart {@code RELATION_VALUE_ERROR}
   */
  RELATION_FILE_ERROR("relation to schema %s has no matching file") {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkArgument(params != null);
      checkArgument(params.length == 1);
      checkArgument(params[0] instanceof String);
      return ImmutableMap.of(ErrorParameterKey.SCHEMA, params[0]);
    }
  }, //
  /**
   * No matching file for referencING schema (only applicable if relation is set to bidirectional).
   * <p>
   * Example: for specimen, if sample is missing (but not if surgery is missing, as the relation from surgery is not
   * bidirectional)
   * <p>
   * Not quite the file-counterpart to {@code RELATION_PARENT_VALUE_ERROR}
   */
  REVERSE_RELATION_FILE_ERROR("relation from schema %s has no matching file and this relation imposes that there be one") {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkArgument(params != null);
      checkArgument(params.length == 1);
      checkArgument(params[0] instanceof String);
      return ImmutableMap.of(ErrorParameterKey.SCHEMA, params[0]);
    }
  }, //
  /**
   * Repeated field names found in header.
   */
  DUPLICATE_HEADER_ERROR("duplicate header found: %s") {
    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkArgument(params != null);
      checkArgument(params.length == 1);
      checkArgument(params[0] instanceof List);
      return ImmutableMap.of(ErrorParameterKey.FIELDS, params[0]);
    }
  };

  private final String message;

  private final boolean structural;

  public abstract ImmutableMap<ErrorParameterKey, Object> build(@Nullable Object... params);

  ValidationErrorCode(String message) {
    this(message, false);
  }

  ValidationErrorCode(String message, boolean structural) {
    this.message = message;
    this.structural = structural;
  }

  public String format(Map<ErrorParameterKey, ? extends Object> parameters) {
    // The formatted message doesn't make sense anymore since the column name was moved to TupleError
    // return String.format(message, terms);
    return this.message;
  }

  public boolean isStructural() {
    return structural;
  }

  public static String format(TupleError error) {
    checkArgument(error != null);
    return error.getCode().format(error.getParameters());
  }
}
