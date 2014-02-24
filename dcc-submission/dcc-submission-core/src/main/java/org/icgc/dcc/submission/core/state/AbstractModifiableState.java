package org.icgc.dcc.submission.core.state;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.submission.core.model.SubmissionFile;

import com.google.common.base.Optional;

/**
 * A state that allows file system modifications of the associated submission.
 */
public abstract class AbstractModifiableState extends AbstractClosePreservingState {

  @Override
  public final boolean isReadOnly() {
    // Not read-only
    return false;
  }

  @Override
  public void modifyFile(@NonNull StateContext context, @NonNull Optional<SubmissionFile> submissionFile) {
    // Current submission data state
    val report = context.getReport();
    val submissionFiles = context.getSubmissionFiles();

    // Refresh
    report.refreshFiles(submissionFiles);

    if (submissionFile.isPresent()) {
      // Reset this data type's reports if its is managed
      val fileType = submissionFile.get().getFileType();
      val managed = fileType != null;
      if (managed) {
        report.resetDataTypes(fileType.getDataType());
      }
    } else {
      // Reset all internal reports
      report.resetDataTypes();
    }

    // Transition based on report
    val nextState = getReportedNextState(report);
    context.setState(nextState);
  }

}
