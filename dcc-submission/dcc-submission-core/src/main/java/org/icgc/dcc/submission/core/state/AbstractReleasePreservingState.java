package org.icgc.dcc.submission.core.state;

import static org.icgc.dcc.submission.core.state.States.convert;
import lombok.val;

import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;

/**
 * A state in which the associated submission must be reset upon release.
 */
public abstract class AbstractReleasePreservingState extends AbstractState {

  @Override
  public Submission performRelease(StateContext context, Release nextRelease) {
    // Preserve the state of the submmssion
    val validSubmission = createPreservedSubmission(context, nextRelease);
    validSubmission.setReport(context.getReport());

    return validSubmission;
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
