package org.icgc.dcc.submission.core.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.SCRIPT_ERROR;
import static org.icgc.dcc.submission.core.report.FieldErrorReport.MAXIMUM_NUM_STORED_ERRORS;
import lombok.val;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class FieldErrorReportTest {

  @Test
  public void testAddErrors() {
    val fieldNames = Lists.newArrayList("f1");
    val parameters = Maps.<ErrorParameterKey, Object> newHashMap();
    val fieldErrorReport = new FieldErrorReport(fieldNames, parameters);

    for (int i = 0; i < MAXIMUM_NUM_STORED_ERRORS * 2; i++) {
      fieldErrorReport.addError(
          error().type(SCRIPT_ERROR).number(0).fieldNames(fieldNames).lineNumber(i).value("v1").build());

    }

    assertThat(fieldErrorReport.getCount()).isEqualTo(MAXIMUM_NUM_STORED_ERRORS * 2);
    assertThat(fieldErrorReport.getValues()).hasSize(MAXIMUM_NUM_STORED_ERRORS);
    assertThat(fieldErrorReport.getValues()).hasSameSizeAs(fieldErrorReport.getLineNumbers());

  }

}
