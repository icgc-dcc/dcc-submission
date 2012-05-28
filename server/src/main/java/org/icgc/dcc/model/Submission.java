package org.icgc.dcc.model;

import com.google.code.morphia.annotations.Embedded;

@Embedded
public class Submission extends BaseEntity {

  @Embedded
  protected Project project;

  protected SubmissionState state;
}
