package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.icgc.dcc.dictionary.model.ValueType;
import org.icgc.dcc.validation.cascading.TupleState.TupleError;

import com.google.common.collect.ImmutableMap;

public enum ValidationErrorCode {

  UNKNOWN_COLUMNS_WARNING("value for unknown column: %s") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  }, //
  STRUCTURALLY_INVALID_ROW_ERROR("structurally invalid row: %s columns against %s declared in the header (row will be ignored by the rest of validation)", true) {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      checkArgument(params[0] instanceof Integer);
      return ImmutableMap.of(ACTUAL_NUM_COLUMNS, params[0]);
    }
  }, //
  RELATION_ERROR("invalid value(s) (%s) for field(s) %s.%s. Expected to match value(s) in: %s.%s") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      checkArgument(params[0] instanceof String);
      checkArgument(params[1] instanceof List);
      return ImmutableMap.of(RELATION_SCHEMA, params[0], RELATION_COLUMNS, params[1]);
    }
  }, //
  RELATION_PARENT_ERROR("no corresponding values in %s.%s for value(s) %s in %s.%s") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      checkArgument(params[0] instanceof String);
      checkArgument(params[1] instanceof List);
      return ImmutableMap.of(RELATION_SCHEMA, params[0], RELATION_COLUMNS, params[1]);
    }
  }, //
  UNIQUE_VALUE_ERROR("invalid set of values (%s) for fields %s. Expected to be unique") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      checkArgument(params[0] instanceof Long);
      return ImmutableMap.of(FIRST_OFFSET, params[0]);
    }
  }, //
  VALUE_TYPE_ERROR("invalid value (%s) for field %s. Expected type is: %s") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      checkArgument(params[0] instanceof ValueType);
      return ImmutableMap.of(EXPECTED_TYPE, params[0]);
    }
  }, //
  OUT_OF_RANGE_ERROR("number %d is out of range for field %s. Expected value between %d and %d") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      checkArgument(params[0] instanceof Long);
      checkArgument(params[1] instanceof Long);

      return ImmutableMap.of(MIN_RANGE, params[0], MAX_RANGE, params[1]);
    }
  }, //
  NOT_A_NUMBER_ERROR("%s is not a number for field %s. Expected a number") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  }, //
  MISSING_VALUE_ERROR("value missing for required field: %s") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  }, //
  CODELIST_ERROR("invalid value %s for field %s. Expected code or value from CodeList %s") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      return ImmutableMap.of(EXPECTED_VALUE, params[0]);
    }
  }, //
  DISCRETE_VALUES_ERROR("invalid value %s for field %s. Expected one of the following values: %s") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      checkArgument(params[0] instanceof Set);

      return ImmutableMap.of(EXPECTED_VALUE, params[0]);
    }
  }, //
  TOO_MANY_FILES_ERROR("more than one file matches the schema pattern") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  }, //
  INVALID_RELATION_ERROR("relation to schema %s has no matching file") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  }, //
  INVALID_REVERSE_RELATION_ERROR("relation from schema %s has no matching file and this relation imposes that there be one") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  }, //
  DUPLICATE_HEADER_ERROR("duplicate header found: %s") {
    @Override
    public final ImmutableMap<String, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  };

  private static final String EXPECTED_VALUE = "expectedValue";

  private static final String EXPECTED_TYPE = "expectedType";

  private static final String FIRST_OFFSET = "firstOffset";

  private static final String MIN_RANGE = "minRange";

  private static final String MAX_RANGE = "maxRange";

  private static final String ACTUAL_NUM_COLUMNS = "actualNumColumns";

  private static final String RELATION_SCHEMA = "relationSchema";

  private static final String RELATION_COLUMNS = "relationColumnNames";

  public static final String FILE_LEVEL_ERROR = "FileLevelError";

  private final String message;

  private final boolean structural;

  public abstract ImmutableMap<String, Object> build(@Nullable Object... params);

  ValidationErrorCode(String message) {
    this(message, false);
  }

  ValidationErrorCode(String message, boolean structural) {
    this.message = message;
    this.structural = structural;
  }

  public String format(Map<String, ? extends Object> parameters) {
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
