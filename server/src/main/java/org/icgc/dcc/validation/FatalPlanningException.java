package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

public class FatalPlanningException extends RuntimeException {

  private final String projectKey;

  private final Plan plan;

  public FatalPlanningException(String projectKey, Plan plan) {
    checkArgument(projectKey != null);
    checkArgument(plan != null);
    this.projectKey = projectKey;
    this.plan = plan;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public Plan getPlan() {
    return plan;
  }
}
