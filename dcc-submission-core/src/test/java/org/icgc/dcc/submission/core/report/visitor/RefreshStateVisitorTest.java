package org.icgc.dcc.submission.core.report.visitor;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.icgc.dcc.core.model.ClinicalType.CLINICAL_CORE_TYPE;
import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.SCRIPT_ERROR;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALIDATING;
import lombok.val;

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.report.DataTypeReport;
import org.icgc.dcc.submission.core.report.DataTypeState;
import org.icgc.dcc.submission.core.report.FileTypeReport;
import org.icgc.dcc.submission.core.report.FileTypeState;
import org.icgc.dcc.submission.core.report.Report;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class RefreshStateVisitorTest {

  @Test
  public void testRefreshState() {
    // Create a new, empty report with no history
    val report = new Report();

    // Simulate clinical-only "submission" process
    report.addFile(FileType.DONOR_TYPE, "donor.txt");
    report.addFile(FileType.SPECIMEN_TYPE, "specimen.txt");
    report.addFile(FileType.SAMPLE_TYPE, "sample.txt");

    // Simulate validating state
    report.inheritState(VALIDATING, dataTypes(CLINICAL_CORE_TYPE));

    // Add an error on the validating data type
    report.addError(error().fileName("donor.txt").type(SCRIPT_ERROR).params("p1", "p2").build());

    // Execute the visitor under test
    val refreshState = new RefreshStateVisitor();
    report.accept(refreshState);

    // Assert via vistor
    report.accept(new NoOpVisitor() {

      @Override
      public void visit(Report report) {
        // Since we had an error
        assertThat(report.isValid()).isFalse();
      }

      @Override
      public void visit(DataTypeReport dataTypeReport) {
        // Since there is only one data type and it had an error
        assertThat(dataTypeReport.getDataTypeState()).as(dataTypeReport.toString())
            .isSameAs(DataTypeState.INVALID);
      }

      @Override
      public void visit(FileTypeReport fileTypeReport) {
        if (fileTypeReport.getFileType() == FileType.DONOR_TYPE) {
          // Since this is the file type the error occurred on
          assertThat(fileTypeReport.getFileTypeState()).as(fileTypeReport.toString())
              .isSameAs(FileTypeState.INVALID);
        } else {
          // And these well valid by virtue of being validated and having no errors
          assertThat(fileTypeReport.getFileTypeState()).as(fileTypeReport.toString())
              .isSameAs(FileTypeState.VALID);
        }
      }

    });

  }

  /**
   * DSL wrapper.
   */
  private static ImmutableList<DataType> dataTypes(DataType... dataTypes) {
    return copyOf(dataTypes);
  }

}
