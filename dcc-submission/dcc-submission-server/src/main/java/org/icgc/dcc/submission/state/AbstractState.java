package org.icgc.dcc.submission.state;

import static org.icgc.dcc.submission.state.States.convert;
import lombok.NonNull;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.validation.ValidationOutcome;
import org.icgc.dcc.submission.validation.core.SubmissionReport;

import com.google.common.base.Optional;

public abstract class AbstractState implements State {

  @Override
  public String getName() {
    return convert(this).name();
  }

  @Override
  public void modifySubmission(@NonNull StateContext context, @NonNull Optional<Path> path) {
    throw new InvalidStateException(this, "modifySubmission");
  }

  @Override
  public void queueRequest(@NonNull StateContext context) {
    throw new InvalidStateException(this, "queueRequest");
  }

  @Override
  public void startValidation(@NonNull StateContext context) {
    throw new InvalidStateException(this, "startValidation");
  }

  @Override
  public void finishValidation(@NonNull StateContext context, @NonNull ValidationOutcome outcome,
      @NonNull SubmissionReport submissionReport) {
    throw new InvalidStateException(this, "finishValidation");
  }

  @Override
  public void signOff(@NonNull StateContext context) {
    throw new InvalidStateException(this, "signOff");
  }

  @Override
  public Submission performRelease(@NonNull StateContext context, @NonNull Release nextRelease) {
    throw new InvalidStateException(this, "performRelease");
  }

}
