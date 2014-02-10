package org.icgc.dcc.submission.core.state;

import lombok.NonNull;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.model.SubmissionFile;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;

import com.google.common.base.Optional;

public abstract class AbstractState implements State {

  @Override
  public String getName() {
    return States.convert(this).name();
  }

  @Override
  public void initializeSubmission(@NonNull StateContext context) {
    throw new InvalidStateException(this, "initializeSubmission");
  }

  @Override
  public void modifySubmission(@NonNull StateContext context, @NonNull Optional<Path> filePath) {
    context.setState(SubmissionState.NOT_VALIDATED);

    val report = context.getReport();
    val submissionFiles = context.getSubmissionFiles();

    // Refresh
    report.updateFiles(submissionFiles);

    if (filePath.isPresent()) {
      // Reset this data type's reports if its is managed
      val dataType = getDataType(submissionFiles, filePath.get());
      val managed = dataType != null;
      if (managed) {
        report.reset(dataType);
      }
    } else {
      // Reset all data types' reports
      report.reset();
    }
  }

  @Override
  public void queueRequest(@NonNull StateContext context, @NonNull Iterable<DataType> dataTypes) {
    throw new InvalidStateException(this, "queueRequest");
  }

  @Override
  public void startValidation(@NonNull StateContext context, @NonNull Iterable<DataType> dataTypes,
      @NonNull Report nextReport) {
    throw new InvalidStateException(this, "startValidation");
  }

  @Override
  public void cancelValidation(@NonNull StateContext context, @NonNull Iterable<DataType> dataTypes) {
    throw new InvalidStateException(this, "cancelValidation");
  }

  @Override
  public void finishValidation(@NonNull StateContext context, @NonNull Outcome outcome, @NonNull Report newReport) {
    throw new InvalidStateException(this, "finishValidation");
  }

  @Override
  public void signOff(@NonNull StateContext context) {
    throw new InvalidStateException(this, "signOff");
  }

  @Override
  public Submission performRelease(StateContext context, Release nextRelease) {
    val nextSubmission =
        new Submission(context.getProjectKey(), context.getProjectName(), nextRelease.getName(),
            SubmissionState.NOT_VALIDATED);
    nextSubmission.setReport(new Report(context.getSubmissionFiles()));

    return nextSubmission;
  }

  private static DataType getDataType(@NonNull Iterable<SubmissionFile> submissionFiles, @NonNull Path filePath) {
    val submissionFile = getSubmissionFile(submissionFiles, filePath);
    val fileType = submissionFile.getFileType();

    return fileType == null ? null : fileType.getDataType();
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

    throw new IllegalArgumentException(filePath + " not found in " + submissionFiles);
  }

}
