package org.icgc.dcc.submission.core.report;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_P_TYPE;
import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.SCRIPT_ERROR;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

@Slf4j
public class FileReportTest {

  @Test
  public void testAddErrors() {
    val fileName = "ssm_p.txt";
    val fileReport = new FileReport(fileName, SSM_P_TYPE);

    fileReport.addError(
        error().fileName(fileName).type(SCRIPT_ERROR).number(0).fieldNames("f1").lineNumber(1).value("v1").build());
    fileReport.addError(
        error().fileName(fileName).type(SCRIPT_ERROR).number(0).fieldNames("f1").lineNumber(2).value("v2").build());
    fileReport.addError(
        error().fileName(fileName).type(SCRIPT_ERROR).number(1).fieldNames("f1").lineNumber(1).value("v1").build());
    fileReport.addError(
        error().fileName(fileName).type(SCRIPT_ERROR).number(1).fieldNames("f1").lineNumber(2).value("v2").build());

    assertThat(fileReport.getErrorReports()).hasSize(2);

    log.info("File report:\n{}", fileReport);
  }

}
