package org.icgc.dcc.submission.core.state;

import static org.icgc.dcc.submission.core.state.States.convert;

import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;

import lombok.val;

/**
 * A state in which the associated submission must be copied and reset upon release.
 */
public abstract class AbstractClosePreservingState extends AbstractState {

  @Override
  public Submission closeRelease(StateContext context, Release nextRelease) {
    // Preserve the state of the submission when copying
    val preservedSubmission = createPreservedSubmission(context, nextRelease);
    preservedSubmission.setReport(context.getReport());

    return preservedSubmission;
  }

  //
  // Helpers
  //

  private Submission createPreservedSubmission(StateContext context, Release nextRelease) {
    return new Submission(
        context.getProjectKey(),
        context.getProjectName(),
        nextRelease.getName(),
        convert(this));
  }

}
