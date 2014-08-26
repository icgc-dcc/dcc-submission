package org.icgc.dcc.submission.reporter.cascading.subassembly.projectDataTypeEntity;

import static cascading.tuple.Fields.NONE;
import static com.google.common.collect.Iterables.toArray;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.hadoop.cascading.Fields2.keyValuePair;
import static org.icgc.dcc.submission.reporter.OutputType.DONOR;
import static org.icgc.dcc.submission.reporter.OutputType.OBSERVATION;
import static org.icgc.dcc.submission.reporter.OutputType.SAMPLE;
import static org.icgc.dcc.submission.reporter.OutputType.SPECIMEN;
import static org.icgc.dcc.submission.reporter.Reporter.ALL_TYPES;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SPECIMEN_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.TYPE_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.getTemporaryCountByFields;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.hadoop.cascading.SubAssemblies.Insert;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.NamingPipe;

import cascading.pipe.HashJoin;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.pipe.joiner.InnerJoin;
import cascading.tuple.Fields;

import com.google.common.collect.ImmutableList;

public class ProjectDataTypeEntity extends SubAssembly {

  public ProjectDataTypeEntity(
      @NonNull final String releaseName,
      @NonNull final String projectKey,
      @NonNull final Pipe preComputationTable) {
    val preProcessedAll = PreProcessing.preProcess(
        preComputationTable,
        PROJECT_ID_FIELD);

    val preProcessedFeatureTypes = PreProcessing.preProcess(
        preComputationTable,
        PROJECT_ID_FIELD.append(TYPE_FIELD));

    setTails(new Merge(

        // All
        new NamingPipe(
            ALL_TYPES,
            new Insert(
                keyValuePair(
                    TYPE_FIELD,
                    ALL_TYPES),
                preProcessedAll)),

        // Feature types
        new NamingPipe(
            "observations",
            preProcessedFeatureTypes)));
  }

  @NoArgsConstructor(access = PRIVATE)
  private static final class PreProcessing {

    private static Pipe preProcess(
        @NonNull final Pipe preComputationTable,
        @NonNull final Fields countByFields) {
      val temporaryDonorCountByFields = getTemporaryCountByFields(countByFields, DONOR);
      val temporarySpecimenCountByFields = getTemporaryCountByFields(countByFields, SPECIMEN);
      val temporarySampleCountByFields = getTemporaryCountByFields(countByFields, SAMPLE);
      val temporaryObservationCountByFields = getTemporaryCountByFields(countByFields, OBSERVATION);

      return new Rename(
          new Retain(
              new HashJoin( // TODO: rewrite as SumBys?
                  // TODO: create more readable subassembly for multi-joins
                  toArray(
                      ImmutableList.<Pipe> builder()

                          .add(ClinicalUniqueCounts.donors(preComputationTable, countByFields))
                          .add(ClinicalUniqueCounts.specimens(preComputationTable, countByFields))
                          .add(ClinicalUniqueCounts.samples(preComputationTable, countByFields))

                          .add(ObservationCounts.observations(preComputationTable, countByFields))

                          .build(),
                      Pipe.class),
                  new Fields[] {
                      temporaryDonorCountByFields,
                      temporarySpecimenCountByFields,
                      temporarySampleCountByFields,
                      temporaryObservationCountByFields },
                  NONE
                      .append(DONOR_UNIQUE_COUNT_FIELD).append(temporaryDonorCountByFields)
                      .append(SPECIMEN_UNIQUE_COUNT_FIELD.append(temporarySpecimenCountByFields))
                      .append(SAMPLE_UNIQUE_COUNT_FIELD.append(temporarySampleCountByFields))
                      .append(_ANALYSIS_OBSERVATION_COUNT_FIELD.append(temporaryObservationCountByFields)),
                  new InnerJoin()),
              temporaryDonorCountByFields // Arbitrarily chosen among the others (renamed further down)
                  .append(DONOR_UNIQUE_COUNT_FIELD)
                  .append(SPECIMEN_UNIQUE_COUNT_FIELD)
                  .append(SAMPLE_UNIQUE_COUNT_FIELD)
                  .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)),
          temporaryDonorCountByFields,
          countByFields);
    }
  }

}
