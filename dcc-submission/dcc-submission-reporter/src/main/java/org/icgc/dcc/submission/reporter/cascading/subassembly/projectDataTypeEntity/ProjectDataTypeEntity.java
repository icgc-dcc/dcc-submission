package org.icgc.dcc.submission.reporter.cascading.subassembly.projectDataTypeEntity;

import static cascading.tuple.Fields.NONE;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Iterables.transform;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.ClinicalType.CLINICAL;
import static org.icgc.dcc.core.model.ClinicalType.CLINICAL_CORE_TYPE;
import static org.icgc.dcc.hadoop.cascading.Fields2.checkFieldsCardinalityOne;
import static org.icgc.dcc.hadoop.cascading.Fields2.getFieldName;
import static org.icgc.dcc.hadoop.cascading.Fields2.keyValuePair;
import static org.icgc.dcc.submission.reporter.OutputType.DONOR;
import static org.icgc.dcc.submission.reporter.OutputType.OBSERVATION;
import static org.icgc.dcc.submission.reporter.OutputType.SAMPLE;
import static org.icgc.dcc.submission.reporter.OutputType.SPECIMEN;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.PROJECT_DATA_TYPE_ENTITY_COUNT_FIELDS;
import static org.icgc.dcc.submission.reporter.ReporterFields.PROJECT_DATA_TYPE_ENTITY_RESULT_FIELDS;
import static org.icgc.dcc.submission.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SPECIMEN_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.TYPE_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.getTemporaryCountByFields;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Insert;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReorderFields;
import org.icgc.dcc.submission.reporter.cascading.subassembly.ClinicalCounts;
import org.icgc.dcc.submission.reporter.cascading.subassembly.ObservationCounts;

import cascading.operation.expression.ExpressionFilter;
import cascading.pipe.Each;
import cascading.pipe.HashJoin;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.AggregateBy;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.pipe.assembly.SumBy;
import cascading.pipe.joiner.InnerJoin;
import cascading.tuple.Fields;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

public class ProjectDataTypeEntity extends SubAssembly {

  public ProjectDataTypeEntity(@NonNull final Pipe preComputationTable) {
    setTails(new Merge(

        // Clinical
        Clinical.process(
            PreProcessing.preProcess(
                preComputationTable,
                PROJECT_ID_FIELD)),

        // Feature types
        FeatureTypes.process(
            PreProcessing.preProcess(
                preComputationTable,
                PROJECT_ID_FIELD.append(TYPE_FIELD)))));
  }

  @NoArgsConstructor(access = PRIVATE)
  private static final class PreProcessing {

    private static Pipe preProcess(
        @NonNull final Pipe preComputationTable,
        @NonNull final Fields countByFields) {
      return new Rename(
          new Retain(
              new HashJoin(
                  // TODO: create more readable subassembly
                  toArray(
                      ImmutableList.<Pipe> builder()

                          .add(ClinicalCounts.donor(preComputationTable, countByFields))
                          .add(ClinicalCounts.specimen(preComputationTable, countByFields))
                          .add(ClinicalCounts.sample(preComputationTable, countByFields))

                          .add(ObservationCounts.observations(preComputationTable, countByFields))

                          .build()
                      , Pipe.class),
                  new Fields[] {
                      getTemporaryCountByFields(countByFields, DONOR),
                      getTemporaryCountByFields(countByFields, SPECIMEN),
                      getTemporaryCountByFields(countByFields, SAMPLE),
                      getTemporaryCountByFields(countByFields, OBSERVATION) },
                  NONE
                      .append(DONOR_UNIQUE_COUNT_FIELD)
                      .append(getTemporaryCountByFields(countByFields, DONOR))
                      .append(SPECIMEN_UNIQUE_COUNT_FIELD.append(getTemporaryCountByFields(countByFields, SPECIMEN)))
                      .append(SAMPLE_UNIQUE_COUNT_FIELD.append(getTemporaryCountByFields(countByFields, SAMPLE)))
                      .append(_ANALYSIS_OBSERVATION_COUNT_FIELD.append(
                          getTemporaryCountByFields(countByFields, OBSERVATION))),
                  new InnerJoin()),
              getTemporaryCountByFields(countByFields, DONOR)
                  .append(DONOR_UNIQUE_COUNT_FIELD)
                  .append(SPECIMEN_UNIQUE_COUNT_FIELD)
                  .append(SAMPLE_UNIQUE_COUNT_FIELD)
                  .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)),
          getTemporaryCountByFields(countByFields, DONOR),
          countByFields);
    }

  }

  @NoArgsConstructor(access = PRIVATE)
  private static final class Clinical {

    private static Pipe process(@NonNull final Pipe preProcessed) {
      val clinicalPipe = new Pipe(CLINICAL_CORE_TYPE.getTypeName(), preProcessed);

      return new ReorderFields(
          new Insert(
              keyValuePair(
                  TYPE_FIELD,
                  CLINICAL),
              new AggregateBy(
                  clinicalPipe,
                  PROJECT_ID_FIELD,
                  toArray(
                      getSumBys(clinicalPipe),
                      AggregateBy.class))),
          PROJECT_DATA_TYPE_ENTITY_RESULT_FIELDS);
    }

    private static Iterable<AggregateBy> getSumBys(@NonNull final Pipe preProcessed) {
      return ImmutableList.copyOf(transform(
          PROJECT_DATA_TYPE_ENTITY_COUNT_FIELDS,
          new Function<Fields, AggregateBy>() {

            @Override
            public AggregateBy apply(Fields countField) {
              return new SumBy(
                  preProcessed,
                  PROJECT_ID_FIELD,
                  checkFieldsCardinalityOne(countField),
                  countField,
                  long.class);
            }

          }));
    }
  }

  @NoArgsConstructor(access = PRIVATE)
  private static final class FeatureTypes {

    private static final String EXCLUDE_CLINICAL_ONLY_TYPE = getFieldName(TYPE_FIELD) + " == null";

    private static Pipe process(Pipe preProcessed) {
      return new Each(
          new Pipe(
              FeatureType.class.getSimpleName(),
              preProcessed),
          new ExpressionFilter(
              EXCLUDE_CLINICAL_ONLY_TYPE,
              String.class));
    }

  }

}
