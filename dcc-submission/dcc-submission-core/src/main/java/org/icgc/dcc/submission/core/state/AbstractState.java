package org.icgc.dcc.submission.core.state;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.fs.SubmissionFileEvent;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;

/**
 * Root of the implementation hierarchy that defines a taxonomy of states.
 * <p>
 * Disallows all actions (except reset) by default. Implementors must override methods to change the default behavior.
 */
public abstract class AbstractState implements State {

  @Override
  public String getName() {
    return States.convert(this).name();
  }

  @Override
  public void initialize(@NonNull StateContext context) {
    throw new InvalidStateException(this, "initializeSubmission");
  }

  @Override
  public void modifyFile(@NonNull StateContext context, @NonNull SubmissionFileEvent event) {
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
  public Submission closeRelease(StateContext context, Release nextRelease) {
    throw new InvalidStateException(this, "performRelease");
  }

  /**
   * Resets all mutable submission data. Available in any state.
   */
  @Override
  public void reset(StateContext context) {
    // Reset it all, the whole lot
    val report = context.getReport();
    report.refreshFiles(context.getSubmissionFiles());
    report.resetAll();

    // Reset to the default state
    context.setState(SubmissionState.getDefaultState());
  }

  //
  // Helpers
  //

  /**
   * Derives the submission state from the supplied {@code report}.
   * <p>
   * Useful in situations when the next state only depends on the {@code report}.
   */
  protected static SubmissionState getReportedNextState(@NonNull Report report) {
    // Transition
    if (report.isValid()) {
      return SubmissionState.VALID;
    } else if (report.hasErrors()) {
      return SubmissionState.INVALID;
    } else {
      return SubmissionState.NOT_VALIDATED;
    }
  }

}
