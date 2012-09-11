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
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(value instanceof List);

      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line);
    }
  }, //
  STRUCTURALLY_INVALID_ROW_ERROR("structurally invalid row: %s columns against %s declared in the header (row will be ignored by the rest of validation)", true) {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(value instanceof Integer);
      checkArgument(params[0] instanceof Integer);

      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line, "actualNumColumns", params[0]);
    }
  }, //
  RELATION_ERROR("invalid value(s) (%s) for field(s) %s.%s. Expected to match value(s) in: %s.%s") {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(value instanceof List);

      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line);
    }
  }, //
  RELATION_PARENT_ERROR("no corresponding values in %s.%s for value(s) %s in %s.%s") {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(value instanceof List);

      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line);
    }
  }, //
  UNIQUE_VALUE_ERROR("invalid set of values (%s) for fields %s. Expected to be unique") {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(value instanceof List);
      checkArgument(line instanceof Long);
      checkArgument(params[0] instanceof Long);

      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line, FIRST_OFFSET, params[0]);
    }
  }, //
  VALUE_TYPE_ERROR("invalid value (%s) for field %s. Expected type is: %s") {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(value instanceof String);
      checkArgument(line instanceof Long);
      checkArgument(params[0] instanceof ValueType);

      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line, EXPECTED_TYPE, params[0]);
    }
  }, //
  OUT_OF_RANGE_ERROR("number %d is out of range for field %s. Expected value between %d and %d") {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(value instanceof Long);
      checkArgument(line instanceof Long);
      checkArgument(params[0] instanceof Long);
      checkArgument(params[1] instanceof Long);

      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line, MIN_RANGE, params[0], MAX_RANGE,
          params[1]);
    }
  }, //
  NOT_A_NUMBER_ERROR("%s is not a number for field %s. Expected a number") {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(value instanceof String);
      checkArgument(line instanceof Long);

      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value);
    }
  }, //
  MISSING_VALUE_ERROR("value missing for required field: %s") {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(line instanceof Long);

      value = value == null ? "" : value;
      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line);
    }
  }, //
  CODELIST_ERROR("invalid value %s for field %s. Expected code or value from CodeList %s") {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(value instanceof String);
      checkArgument(line instanceof Long);

      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line, EXPECTED_VALUE, params[0]);
    }
  }, //
  DISCRETE_VALUES_ERROR("invalid value %s for field %s. Expected one of the following values: %s") {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(value instanceof String);
      checkArgument(line instanceof Long);
      checkArgument(params[0] instanceof Set);

      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line, EXPECTED_VALUE, params[0]);
    }
  }, //
  TOO_MANY_FILES_ERROR("more than one file matches the schema pattern") {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(value instanceof List);
      checkArgument(line instanceof Long);

      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line);
    }
  }, //
  INVALID_RELATION_ERROR("a required schema for this relation was not found") {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(value instanceof String);
      checkArgument(line instanceof Long);

      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line);
    }
  }, //
  MISSING_SCHEMA_ERROR("no valid schema found") {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(line instanceof Long);

      value = value == null ? "" : value;
      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line);
    }
  }, //
  DUPLICATE_HEADER_ERROR("duplicate header found: %s") {
    @Override
    public final ImmutableMap<String, Object> build(String columnName, Object value, Long line, Object... params) {
      checkArgument(columnName instanceof String);
      checkArgument(line instanceof Long);

      value = value == null ? "" : value;
      return ImmutableMap.of(COLUMN_NAME, columnName, VALUE, value, LINE, line);
    }
  };

  private static final String COLUMN_NAME = "columnName";

  private static final String VALUE = "value";

  private static final String LINE = "line";

  private static final String EXPECTED_VALUE = "expectedValue";

  private static final String EXPECTED_TYPE = "expectedType";

  private static final String FIRST_OFFSET = "firstOffset";

  private static final String MIN_RANGE = "minRange";

  private static final String MAX_RANGE = "maxRange";

  private final String message;

  private final boolean structural;

  public abstract ImmutableMap<String, Object> build(String columnName, Object value, Long line,
      @Nullable Object... params);

  ValidationErrorCode(String message) {
    this(message, false);
  }

  ValidationErrorCode(String message, boolean structural) {
    this.message = message;
    this.structural = structural;
  }

  public String format(Map<String, ? extends Object> parameters) {
    return String.format(message, parameters);
  }

  public boolean isStructural() {
    return structural;
  }

  public static String format(TupleError error) {
    checkArgument(error != null);
    return error.getCode().format(error.getParameters());
  }
}
