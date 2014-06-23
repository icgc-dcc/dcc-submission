package org.icgc.dcc.reporter;

import static com.google.common.collect.Iterables.toArray;
import static org.icgc.dcc.hadoop.cascading.Fields2.checkFieldsCardinalityOne;
import static org.icgc.dcc.hadoop.cascading.Fields2.getCountFieldCounterpart;
import static org.icgc.dcc.reporter.OutputType.DONOR;
import static org.icgc.dcc.reporter.OutputType.OBSERVATION;
import static org.icgc.dcc.reporter.OutputType.SAMPLE;
import static org.icgc.dcc.reporter.OutputType.SEQUENCING_STRATEGY;
import static org.icgc.dcc.reporter.OutputType.SPECIMEN;
import static org.icgc.dcc.reporter.ReporterFields.ANALYSIS_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SAMPLE_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SPECIMEN_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.TYPE_FIELD;
import static org.icgc.dcc.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;

import org.icgc.dcc.hadoop.cascading.SubAssemblies.GroupBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.GroupBy.GroupByData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.NamingPipe;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Sum;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.UniqueCountBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.UniqueCountBy.UniqueCountByData;

import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Retain;
import cascading.pipe.assembly.Unique;
import cascading.tuple.Fields;

import com.google.common.collect.ImmutableList;

public class StatsGathering extends SubAssembly {

  StatsGathering(Pipe preComputationTable) {
    setTails(toArray(
        process(preComputationTable),
        Pipe.class));
  }

  private static Iterable<Pipe> process(Pipe preComputationTable) {
    return ImmutableList.<Pipe> builder()

        .add(processDonors(preComputationTable))
        .add(processSpecimens(preComputationTable))
        .add(processSamples(preComputationTable))
        .add(processObservations(preComputationTable))
        .add(processSequencingStrategies(preComputationTable))

        .build();
  }

  private static Pipe processDonors(Pipe preComputationTable) {
    return new NamingPipe(
        DONOR,

        new UniqueCountBy(UniqueCountByData.builder()

            .pipe(preComputationTable)
            .uniqueFields(
                PROJECT_ID_FIELD
                    .append(TYPE_FIELD)
                    .append(DONOR_ID_FIELD))
            .countByFields(
                PROJECT_ID_FIELD
                    .append(TYPE_FIELD))
            .resultCountField(getCountFieldCounterpart(DONOR_ID_FIELD))

            .build()));
  }

  private static Pipe processSpecimens(Pipe preComputationTable) {
    return new NamingPipe(
        SPECIMEN,
        new UniqueCountBy(UniqueCountByData.builder()

            .pipe(preComputationTable)
            .uniqueFields(
                PROJECT_ID_FIELD
                    .append(TYPE_FIELD)
                    .append(SPECIMEN_ID_FIELD))
            .countByFields(
                PROJECT_ID_FIELD
                    .append(TYPE_FIELD))
            .resultCountField(getCountFieldCounterpart(SPECIMEN_ID_FIELD))

            .build()));
  }

  private static Pipe processSamples(Pipe preComputationTable) {
    return new NamingPipe(
        SAMPLE,
        new UniqueCountBy(UniqueCountByData.builder()

            .pipe(preComputationTable)
            .uniqueFields(
                PROJECT_ID_FIELD
                    .append(TYPE_FIELD)
                    .append(SAMPLE_ID_FIELD))
            .countByFields(
                PROJECT_ID_FIELD
                    .append(TYPE_FIELD))
            .resultCountField(getCountFieldCounterpart(SAMPLE_ID_FIELD))

            .build()));
  }

  private static Pipe processObservations(Pipe preComputationTable) {
    return new NamingPipe(
        OBSERVATION,
        getPreCountedUniqueCountPipe(
            preComputationTable,
            ANALYSIS_ID_FIELD,
            _ANALYSIS_OBSERVATION_COUNT_FIELD,
            PROJECT_ID_FIELD
                .append(TYPE_FIELD)));
  }

  private static Pipe processSequencingStrategies(Pipe preComputationTable) {
    return new NamingPipe(
        SEQUENCING_STRATEGY,
        new UniqueCountBy(UniqueCountByData.builder()

            .pipe(preComputationTable)
            .uniqueFields(
                PROJECT_ID_FIELD
                    .append(SEQUENCING_STRATEGY_FIELD)
                    .append(DONOR_ID_FIELD))
            .countByFields(
                PROJECT_ID_FIELD
                    .append(SEQUENCING_STRATEGY_FIELD))
            .resultCountField(getCountFieldCounterpart(SEQUENCING_STRATEGY_FIELD))

            .build()));
  }

  private static Pipe getPreCountedUniqueCountPipe(
      Pipe preComputationTable, Fields targetField, Fields preCountField, Fields groupByFields) {
    checkFieldsCardinalityOne(targetField);
    checkFieldsCardinalityOne(preCountField);

    return

    //
    new Retain(

        //
        new Sum(

            //
            new GroupBy(GroupByData.builder()

                .pipe(

                    //
                    new Unique(
                        preComputationTable,

                        //
                        groupByFields
                            .append(preCountField)
                            .append(targetField)))

                .groupByFields(groupByFields)

                .build()),

            //
            preCountField),

        // Retain fields
        groupByFields
            .append(preCountField));
  }

}
