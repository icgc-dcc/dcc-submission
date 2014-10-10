package org.icgc.dcc.submission.core.report;

import lombok.val;

import org.icgc.dcc.common.core.model.ValueType;
import org.junit.Test;

public class ErrorTypeTest {

  @Test
  public void testBuild() {
    val type = ErrorType.VALUE_TYPE_ERROR;
    type.build(ValueType.INTEGER);
  }

}
