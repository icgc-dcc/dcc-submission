package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import org.icgc.dcc.release.model.QueuedProject;

public class FatalPlanningException extends RuntimeException {

  private final QueuedProject project;

  private final Plan plan;

  public FatalPlanningException(QueuedProject project, Plan plan) {
    checkArgument(project != null);
    checkArgument(plan != null);
    this.project = project;
    this.plan = plan;
  }

  public QueuedProject getProject() {
    return project;
  }

  public Plan getPlan() {
    return plan;
  }
}
