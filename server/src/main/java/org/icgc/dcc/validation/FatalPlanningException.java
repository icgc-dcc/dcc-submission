package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import org.icgc.dcc.release.model.QueuedProject;

public final class FatalPlanningException extends RuntimeException {

  private final QueuedProject project;

  private final Plan plan;

  public FatalPlanningException(QueuedProject queuedProject, Plan plan) {
    super(describe(queuedProject, plan));
    checkArgument(queuedProject != null);
    checkArgument(plan != null);
    this.project = queuedProject;
    this.plan = plan;
  }

  private static String describe(QueuedProject queuedProject, Plan plan) {
    return String.format("project = %s, errors = %s", queuedProject.getKey(), plan.getFileLevelErrors());
  }

  public QueuedProject getProject() {
    return project;
  }

  public Plan getPlan() {
    return plan;
  }
}
