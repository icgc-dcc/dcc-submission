package org.icgc.dcc.submission.core.state;

import static com.google.common.base.Optional.fromNullable;
import lombok.NonNull;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.submission.core.model.SubmissionFile;
import org.icgc.dcc.submission.release.model.SubmissionState;

import com.google.common.base.Optional;

/**
 * A state that allows file system modifications of the associated submission.
 */
public abstract class AbstractModifiableState extends AbstractReleaseResettableState {

  @Override
  public final boolean isReadOnly() {
    // Not read-only
    return false;
  }

  @Override
  public void modifySubmission(@NonNull StateContext context, @NonNull Optional<Path> filePath) {
    context.setState(SubmissionState.NOT_VALIDATED);

    val report = context.getReport();
    val submissionFiles = context.getSubmissionFiles();

    // Refresh
    report.updateFiles(submissionFiles);

    if (filePath.isPresent()) {

      //
      // One data type
      //

      // Reset this data type's reports if its is managed
      val dataType = getDataType(submissionFiles, filePath.get());
      val managed = dataType.isPresent();
      if (managed) {
        report.reset(dataType.get());
      }
    } else {

      //
      // All data types
      //

      // Reset all data types' reports
      report.reset();
    }
  }

  //
  // Helpers
  //

  private static Optional<DataType> getDataType(@NonNull Iterable<SubmissionFile> submissionFiles,
      @NonNull Path filePath) {
    val submissionFile = getSubmissionFile(submissionFiles, filePath);
    val fileType = submissionFile.getFileType();

    return fromNullable(fileType == null ? null : fileType.getDataType());
  }

  private static SubmissionFile getSubmissionFile(@NonNull Iterable<SubmissionFile> submissionFiles,
      @NonNull Path filePath) {
    val fileName = filePath.getName();
    for (val submissionFile : submissionFiles) {
      val match = submissionFile.getName().equals(fileName);
      if (match) {
        return submissionFile;
      }
    }

    throw new IllegalArgumentException("File '" + filePath + "' not found in " + submissionFiles);
  }

}
