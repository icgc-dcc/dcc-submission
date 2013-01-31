package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkNotNull;

public final class FilePresenceException extends Exception {

  private final Plan plan;

  /**
   * Static so can be called from constructor.
   */
  private static String describe(Plan plan) {
    return String.format("project = %s, errors = %s", plan.getQueuedProject().getKey(), plan.getFileLevelErrors());
  }

  public FilePresenceException(Plan plan) {
    super(describe(checkNotNull(plan)));
    this.plan = plan;
  }

  public Plan getPlan() {
    return plan;
  }
}
