package org.icgc.dcc.submission.core.state;

import lombok.NonNull;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;

import com.google.common.base.Optional;

/**
 * Root of the implementation hierarchy that defines a taxonomy of states.
 * <p>
 * Disallows all state transitions by default. Implementors must override methods to change the default behavior.
 */
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
    throw new InvalidStateException(this, "modifySubmission");
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
  public void finishValidation(@NonNull StateContext context, @NonNull Iterable<DataType> dataTypes,
      @NonNull Outcome outcome, @NonNull Report newReport) {
    throw new InvalidStateException(this, "finishValidation");
  }

  @Override
  public void signOff(@NonNull StateContext context) {
    throw new InvalidStateException(this, "signOff");
  }

  @Override
  public Submission performRelease(StateContext context, Release nextRelease) {
    throw new InvalidStateException(this, "performRelease");
  }

}
