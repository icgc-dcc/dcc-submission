package org.icgc.dcc.submission.core.report;

import lombok.val;

import org.icgc.dcc.submission.dictionary.model.ValueType;
import org.junit.Test;

public class ErrorTypeTest {

  @Test
  public void testBuild() {
    val type = ErrorType.VALUE_TYPE_ERROR;
    type.build(ValueType.INTEGER);
  }

}
