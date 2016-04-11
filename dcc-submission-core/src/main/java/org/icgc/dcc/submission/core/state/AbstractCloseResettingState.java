package org.icgc.dcc.submission.core.state;

import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;

import lombok.NonNull;
import lombok.val;

/**
 * A state in which the associated submission must be copied and reset upon release.
 */
public abstract class AbstractCloseResettingState extends AbstractState {

  @Override
  public Submission closeRelease(@NonNull StateContext context, @NonNull Release nextRelease) {
    // Reset (and copy!)
    val resetReport = new Report(context.getSubmissionFiles());

    val resetSubmission = createResetSubmission(context, nextRelease);
    resetSubmission.setReport(resetReport);

    return resetSubmission;
  }

  //
  // Helpers
  //

  private static Submission createResetSubmission(StateContext context, Release nextRelease) {
    return new Submission(
        context.getProjectKey(),
        context.getProjectName(),
        nextRelease.getName(),
        SubmissionState.NOT_VALIDATED);
  }

}
