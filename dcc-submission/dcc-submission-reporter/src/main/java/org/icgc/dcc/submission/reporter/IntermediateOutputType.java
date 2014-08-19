package org.icgc.dcc.submission.reporter;

import static cascading.tuple.Fields.NONE;
import static org.icgc.dcc.core.util.Joiners.UNDERSCORE;
import static org.icgc.dcc.submission.reporter.ReporterFields.ANALYSIS_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.REDUNDANT_SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.RELEASE_NAME_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SPECIMEN_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SPECIMEN_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.TYPE_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.core.model.Identifiable;

import cascading.tuple.Fields;

@RequiredArgsConstructor
public enum IntermediateOutputType implements Identifiable {

  PRE_COMPUTATION(NONE

      .append(RELEASE_NAME_FIELD)
      .append(PROJECT_ID_FIELD)
      .append(TYPE_FIELD)
      .append(DONOR_ID_FIELD)
      .append(SPECIMEN_ID_FIELD)
      .append(SAMPLE_ID_FIELD)
      .append(ANALYSIS_ID_FIELD)
      .append(SEQUENCING_STRATEGY_FIELD)
      .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)),

  PRE_COMPUTATION_TMP1(NONE

      .append(TYPE_FIELD)
      .append(DONOR_ID_FIELD)
      .append(SPECIMEN_ID_FIELD)
      .append(SAMPLE_ID_FIELD)
      .append(ANALYSIS_ID_FIELD)
      .append(SEQUENCING_STRATEGY_FIELD)
      .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)),

  PRE_COMPUTATION_TMP2(NONE

      .append(TYPE_FIELD)
      .append(DONOR_ID_FIELD)
      .append(SPECIMEN_ID_FIELD)
      .append(SAMPLE_ID_FIELD)
      .append(ANALYSIS_ID_FIELD)
      .append(SEQUENCING_STRATEGY_FIELD)
      .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)),

  PRE_COMPUTATION_CLINICAL(NONE

      .append(DONOR_ID_FIELD)
      .append(SPECIMEN_ID_FIELD)
      .append(SAMPLE_ID_FIELD)),

  PRE_COMPUTATION_FEATURE_TYPES(NONE

      .append(TYPE_FIELD)
      .append(ANALYSIS_ID_FIELD)
      .append(REDUNDANT_SAMPLE_ID_FIELD)
      .append(SEQUENCING_STRATEGY_FIELD)
      .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)),

  PRE_PROCESSING_ALL(NONE

      .append(PROJECT_ID_FIELD)
      .append(DONOR_UNIQUE_COUNT_FIELD)
      .append(SPECIMEN_UNIQUE_COUNT_FIELD)
      .append(SAMPLE_UNIQUE_COUNT_FIELD)
      .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)),

  PRE_PROCESSING_FEATURE_TYPES(NONE

      .append(PROJECT_ID_FIELD)
      .append(TYPE_FIELD)
      .append(DONOR_UNIQUE_COUNT_FIELD)
      .append(SPECIMEN_UNIQUE_COUNT_FIELD)
      .append(SAMPLE_UNIQUE_COUNT_FIELD)
      .append(_ANALYSIS_OBSERVATION_COUNT_FIELD));

  @Getter
  private final Fields reorderedFields;

  @Override
  public String getId() {
    return name().toLowerCase();
  }

  public String getPipeName(String projectKey) {
    return UNDERSCORE.join(name(), projectKey);
  }

}
