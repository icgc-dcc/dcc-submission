package org.icgc.dcc.submission.reporter.cascading.subassembly.projectdatatypeentity;

import static cascading.tuple.Fields.NONE;
import static com.google.common.collect.Iterables.toArray;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.hadoop.cascading.Fields2.getFieldName;
import static org.icgc.dcc.hadoop.cascading.Fields2.keyValuePair;
import static org.icgc.dcc.submission.reporter.OutputType.DONOR;
import static org.icgc.dcc.submission.reporter.OutputType.OBSERVATION;
import static org.icgc.dcc.submission.reporter.OutputType.SAMPLE;
import static org.icgc.dcc.submission.reporter.OutputType.SPECIMEN;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.ORDERED_RESULT_FIELDS;
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
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin.JoinData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReorderFields;

import cascading.operation.expression.ExpressionFilter;
import cascading.pipe.Each;
import cascading.pipe.HashJoin;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.pipe.joiner.InnerJoin;
import cascading.pipe.joiner.RightJoin;
import cascading.tuple.Fields;

import com.google.common.collect.ImmutableList;

public class ProjectDataTypeEntity extends SubAssembly {

  public ProjectDataTypeEntity(@NonNull final Pipe preComputationTable) {
    setTails(new Merge(

        // All
        All.process(
            PreProcessing.preProcess(
                preComputationTable,
                PROJECT_ID_FIELD,
                ORDERED_RESULT_FIELDS)),

        // Feature types
        FeatureTypes.process(
            PreProcessing.preProcess(
                preComputationTable,
                PROJECT_ID_FIELD.append(TYPE_FIELD),
                ORDERED_RESULT_FIELDS.append(TYPE_FIELD)))));
  }

  @NoArgsConstructor(access = PRIVATE)
  private static final class PreProcessing {

    private static Pipe preProcess(
        @NonNull final Pipe preComputationTable,
        @NonNull final Fields countByFields,
        @NonNull final Fields reorderedResultFields) {
      val temporaryObservationCountByFields = getTemporaryCountByFields(countByFields, OBSERVATION);

      return new ReorderFields(
          new ReadableHashJoin(JoinData.builder()
              .joiner(new RightJoin())

              .leftPipe(ObservationCounts.observations(preComputationTable, countByFields))
              .leftJoinFields(temporaryObservationCountByFields)

              .rightPipe(preProcessClinical(preComputationTable, countByFields))
              .rightJoinFields(countByFields)

              .resultFields(NONE
                  .append(temporaryObservationCountByFields)
                  .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)
                  .append(countByFields)
                  .append(DONOR_UNIQUE_COUNT_FIELD)
                  .append(SPECIMEN_UNIQUE_COUNT_FIELD)
                  .append(SAMPLE_UNIQUE_COUNT_FIELD))
              .discardFields(temporaryObservationCountByFields)

              .build()),
          reorderedResultFields);
    }

    private static Pipe preProcessClinical(
        @NonNull final Pipe preComputationTable,
        @NonNull final Fields countByFields) {
      val temporaryDonorCountByFields = getTemporaryCountByFields(countByFields, DONOR);
      val temporarySpecimenCountByFields = getTemporaryCountByFields(countByFields, SPECIMEN);
      val temporarySampleCountByFields = getTemporaryCountByFields(countByFields, SAMPLE);

      return new Rename(
          new Retain(
              new HashJoin( // TODO: rewrite as SumBys?
                  // TODO: create more readable subassembly
                  toArray(
                      ImmutableList.<Pipe> builder()

                          .add(ClinicalUniqueCounts.donors(preComputationTable, countByFields))
                          .add(ClinicalUniqueCounts.specimens(preComputationTable, countByFields))
                          .add(ClinicalUniqueCounts.samples(preComputationTable, countByFields))

                          .build(),
                      Pipe.class),
                  new Fields[] {
                      temporaryDonorCountByFields,
                      temporarySpecimenCountByFields,
                      temporarySampleCountByFields },
                  NONE
                      .append(DONOR_UNIQUE_COUNT_FIELD).append(temporaryDonorCountByFields)
                      .append(SPECIMEN_UNIQUE_COUNT_FIELD.append(temporarySpecimenCountByFields))
                      .append(SAMPLE_UNIQUE_COUNT_FIELD.append(temporarySampleCountByFields)),
                  new InnerJoin()),
              temporaryDonorCountByFields // Arbitrarily chosen among the others (renamed further down)
                  .append(DONOR_UNIQUE_COUNT_FIELD)
                  .append(SPECIMEN_UNIQUE_COUNT_FIELD)
                  .append(SAMPLE_UNIQUE_COUNT_FIELD)),
          temporaryDonorCountByFields,
          countByFields);
    }
  }

  @NoArgsConstructor(access = PRIVATE)
  private static final class All {

    private static final String NAME = All.class.getSimpleName();

    private static Pipe process(Pipe preProcessed) {
      return new NamingPipe(
          NAME,
          new Insert(
              keyValuePair(
                  TYPE_FIELD,
                  NAME.toLowerCase()),
              preProcessed));
    }
  }

  @NoArgsConstructor(access = PRIVATE)
  private static final class FeatureTypes {

    private static final String NAME = FeatureTypes.class.getSimpleName();
    private static final String EXCLUDE_CLINICAL_ONLY_TYPE = getFieldName(TYPE_FIELD) + " == null";

    private static Pipe process(Pipe preProcessed) {
      return new NamingPipe(
          NAME,
          new Each(
              preProcessed,
              new ExpressionFilter(
                  EXCLUDE_CLINICAL_ONLY_TYPE,
                  String.class)));
    }

  }

}
