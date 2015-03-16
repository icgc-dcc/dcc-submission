package org.icgc.dcc.submission.reporter.cascading.subassembly;

import static cascading.tuple.Fields.NONE;
import static com.google.common.collect.Iterables.toArray;
import static org.icgc.dcc.common.cascading.Fields2.checkFieldsCardinalityOne;
import static org.icgc.dcc.common.cascading.Fields2.getCountFieldCounterpart;
import static org.icgc.dcc.common.cascading.Fields2.getFieldName;
import static org.icgc.dcc.common.cascading.Fields2.getTemporaryCountByFields;
import static org.icgc.dcc.common.cascading.Fields2.keyValuePair;
import static org.icgc.dcc.submission.reporter.OutputType.DONOR;
import static org.icgc.dcc.submission.reporter.OutputType.OBSERVATION;
import static org.icgc.dcc.submission.reporter.OutputType.SAMPLE;
import static org.icgc.dcc.submission.reporter.OutputType.SPECIMEN;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_TYPE_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SPECIMEN_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SPECIMEN_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.TYPE_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.common.cascading.SubAssemblies.Insert;
import org.icgc.dcc.common.cascading.SubAssemblies.NamingPipe;
import org.icgc.dcc.common.cascading.SubAssemblies.ReorderAllFields;
import org.icgc.dcc.common.cascading.SubAssemblies.UniqueCountBy;
import org.icgc.dcc.common.cascading.SubAssemblies.UniqueCountBy.UniqueCountByData;
import org.icgc.dcc.submission.reporter.OutputType;

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

import com.google.common.collect.ImmutableList;

public class ProjectDataTypeEntity extends SubAssembly {

  public static final String ALL_TYPES = "all";

  public ProjectDataTypeEntity(
      @NonNull final Pipe preComputationTable,
      @NonNull final String releaseName) {
    setTails(new Merge(

        normalizeMergePipe(
            ALL_TYPES,
            aggregate(
                preComputationTable,
                PROJECT_ID_FIELD,
                TYPE_FIELD, SAMPLE_TYPE_FIELD)),

        normalizeMergePipe(
            getFieldName(SAMPLE_TYPE_FIELD),
            aggregate(
                preComputationTable,
                PROJECT_ID_FIELD.append(SAMPLE_TYPE_FIELD),
                TYPE_FIELD)),

        normalizeMergePipe(
            getFieldName(TYPE_FIELD),
            aggregate(
                preComputationTable,
                PROJECT_ID_FIELD.append(TYPE_FIELD),
                SAMPLE_TYPE_FIELD))));
  }

  private Pipe normalizeMergePipe(
      @NonNull final String name,
      @NonNull final Pipe pipe) {
    return new NamingPipe(
        name,
        new ReorderAllFields(
            pipe,
            NONE.append(PROJECT_ID_FIELD)
                .append(TYPE_FIELD)
                .append(SAMPLE_TYPE_FIELD)
                .append(DONOR_UNIQUE_COUNT_FIELD)
                .append(SPECIMEN_UNIQUE_COUNT_FIELD)
                .append(SAMPLE_UNIQUE_COUNT_FIELD)
                .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)));
  }

  private static Pipe aggregate(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields groupByFields,
      @NonNull final Fields... placeholders) {

    val temporaryDonorCountByFields = getTemporaryCountByFields(groupByFields, DONOR);
    val temporarySpecimenCountByFields = getTemporaryCountByFields(groupByFields, SPECIMEN);
    val temporarySampleCountByFields = getTemporaryCountByFields(groupByFields, SAMPLE);
    val temporaryObservationCountByFields = getTemporaryCountByFields(groupByFields, OBSERVATION);

    Pipe pipe = new Rename(
        new Retain(
            new HashJoin(
                // TODO: create more readable subassembly for multi-joins
                toArray(
                    ImmutableList.<Pipe> builder()

                        .add(new Rename(
                            donorUniqueCountBy(preComputationTable, groupByFields),
                            groupByFields, temporaryDonorCountByFields))
                        .add(new Rename(
                            specimenUniqueCountBy(preComputationTable, groupByFields),
                            groupByFields, temporarySpecimenCountByFields))
                        .add(new Rename(
                            sampleUniqueCountBy(preComputationTable, groupByFields),
                            groupByFields, temporarySampleCountByFields))

                        .add(new Rename(
                            observationCountBy(preComputationTable, groupByFields),
                            groupByFields, temporaryObservationCountByFields))

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
        groupByFields);

    // Add placeholder fields
    for (val field : placeholders) {
      pipe = new Insert(
          keyValuePair(
              checkFieldsCardinalityOne(field),
              ALL_TYPES),
          pipe);
    }

    return pipe;
  }

  /**
   * This one is used in the other table as well (hence the public).
   */
  static AggregateBy donorUniqueCountBy(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields countByFields) {
    return clinicalUniqueCountBy(
        new Retain(
            preComputationTable,
            countByFields.append(DONOR_ID_FIELD)),
        DONOR,
        countByFields,
        DONOR_ID_FIELD);
  }

  private static AggregateBy specimenUniqueCountBy(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields countByFields) {
    return clinicalUniqueCountBy(
        new Retain(
            preComputationTable,
            countByFields.append(SPECIMEN_ID_FIELD)),
        SPECIMEN,
        countByFields,
        SPECIMEN_ID_FIELD);
  }

  private static AggregateBy sampleUniqueCountBy(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields countByFields) {
    return clinicalUniqueCountBy(
        new Retain(
            preComputationTable,
            countByFields.append(SAMPLE_ID_FIELD)),
        SAMPLE,
        countByFields,
        SAMPLE_ID_FIELD);
  }

  private static AggregateBy clinicalUniqueCountBy(
      @NonNull final Pipe trimmedPipe,
      @NonNull final OutputType outputType,
      @NonNull final Fields countByFields,
      @NonNull final Fields clinicalIdField) {
    return new UniqueCountBy(
        outputType.getId(),
        UniqueCountByData.builder()

            .pipe(trimmedPipe)
            .uniqueFields(countByFields.append(checkFieldsCardinalityOne(clinicalIdField)))
            .countByFields(countByFields)
            .resultCountField(getCountFieldCounterpart(clinicalIdField))

            .build());
  }

  private static AggregateBy observationCountBy(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields sumByFields) {
    return new SumBy(
        new Retain(
            preComputationTable,
            sumByFields.append(_ANALYSIS_OBSERVATION_COUNT_FIELD)),
        sumByFields,
        _ANALYSIS_OBSERVATION_COUNT_FIELD,
        _ANALYSIS_OBSERVATION_COUNT_FIELD,
        long.class);
  }

}
