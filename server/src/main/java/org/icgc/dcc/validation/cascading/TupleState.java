package org.icgc.dcc.validation.cascading;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.icgc.dcc.validation.ValidationErrorCode;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * Each {@code Tuple} should have one field that holds an instance of this class. It is used to track the state
 * (valid/invalid and corresponding reasons) of the whole {@code Tuple}.
 */
public class TupleState implements Serializable {

  private int offset;

  private List<TupleError> errors;

  private boolean structurallyValid; // to save time on filtering

  public TupleState() {
    structurallyValid = true;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public void reportError(ValidationErrorCode code, @Nullable Object... parameters) {
    checkArgument(code != null);
    ensureErrors().add(new TupleError(code, parameters));
    if(code == ValidationErrorCode.STRUCTURALLY_INVALID_ROW_ERROR) {
      structurallyValid = false;
    }
  }

  public Iterable<TupleError> getErrors() {
    return ensureErrors();
  }

  @JsonIgnore
  public boolean isValid() {
    return errors == null || errors.size() == 0;
  }

  @JsonIgnore
  public boolean isInvalid() {
    return isValid() == false;
  }

  @JsonIgnore
  public boolean isStructurallyValid() {
    return structurallyValid;
  }

  public int getOffset() {
    return offset;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(TupleState.class).add(ValidationFields.OFFSET_FIELD_NAME, offset)
        .add("valid", isValid()).add("errors", errors).toString();
  }

  /**
   * Used to lazily instantiate the errors list. This method never returns {@code null}.
   */
  private List<TupleError> ensureErrors() {
    return errors == null ? (errors = Lists.newArrayListWithExpectedSize(3)) : errors;
  }

  /**
   * Holds an error. The {@code code} uniquely identifies the error (e.g.: range error) and the {@code parameters}
   * capture the error details (e.g.: the expected range; min and max).
   */
  public static final class TupleError implements Serializable {

    private final ValidationErrorCode code;

    private final Object[] parameters;

    public TupleError() {
      code = null;
      parameters = null;
    }

    private TupleError(ValidationErrorCode code, Object... parameters) {
      this.code = code;
      this.parameters = parameters;
    }

    public ValidationErrorCode getCode() {
      return code;
    }

    public Object[] getParameters() {
      return parameters;
    }

    @JsonIgnore
    public String getMessage() {
      return code.format(getParameters());
    }

    @Override
    public String toString() {
      return Objects.toStringHelper(TupleError.class).add("code", code).add("parameters", Arrays.toString(parameters))
          .toString();
    }

  }

}
