package org.icgc.dcc.validation.report;

import org.icgc.dcc.validation.CascadingStrategy;

public interface ReportCollector {
  public Outcome collect(CascadingStrategy strategy, SchemaReport report);
}
