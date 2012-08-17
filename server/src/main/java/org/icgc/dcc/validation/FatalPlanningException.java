package org.icgc.dcc.validation;

import java.util.Map;

import org.icgc.dcc.validation.cascading.TupleState;

public class FatalPlanningException extends RuntimeException {

  private final Map<String, TupleState> errors;

  public FatalPlanningException(Map<String, TupleState> errors) {
    super();
    this.errors = errors;
  }

  public Map<String, TupleState> getErrors() {
    return this.errors;
  }
}
