package org.icgc.dcc.submission.reporter;

import org.icgc.dcc.core.model.Identifiable;

public enum OutputType implements Identifiable {

  DONOR,
  SPECIMEN,
  SAMPLE,
  OBSERVATION,
  SEQUENCING_STRATEGY;

  @Override
  public String getId() {
    return name().toLowerCase();
  }

}
