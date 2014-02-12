package org.icgc.dcc.submission.core.state;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;

/**
 * A state in which the associated submission must be reset upon release.
 */
public abstract class AbstractReleaseResettingState extends AbstractState {

  @Override
  public Submission performRelease(@NonNull StateContext context, @NonNull Release nextRelease) {
    // Reset (and copy!)
    val resetReport = createResetReport(context);

    val resetSubmission = createResetSubmission(context, nextRelease);
    resetSubmission.setReport(resetReport);

    return resetSubmission;
  }

  //
  // Helpers
  //

  private static Report createResetReport(StateContext context) {
    return new Report(context.getSubmissionFiles());
  }

  private static Submission createResetSubmission(StateContext context, Release nextRelease) {
    return new Submission(
        context.getProjectKey(),
        context.getProjectName(),
        nextRelease.getName(),
        SubmissionState.NOT_VALIDATED);
  }

}
