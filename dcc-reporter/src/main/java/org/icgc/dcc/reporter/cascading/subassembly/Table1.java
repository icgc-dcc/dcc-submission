package org.icgc.dcc.reporter.cascading.subassembly;

import static com.google.common.collect.Iterables.toArray;
import static org.icgc.dcc.hadoop.cascading.Fields2.getCountFieldCounterpart;
import static org.icgc.dcc.hadoop.cascading.Fields2.getRedundantFieldCounterpart;
import static org.icgc.dcc.reporter.OutputType.DONOR;
import static org.icgc.dcc.reporter.OutputType.OBSERVATION;
import static org.icgc.dcc.reporter.OutputType.SAMPLE;
import static org.icgc.dcc.reporter.OutputType.SPECIMEN;
import static org.icgc.dcc.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.DONOR_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SAMPLE_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SAMPLE_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SPECIMEN_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SPECIMEN_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.TYPE_FIELD;
import static org.icgc.dcc.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;

import org.icgc.dcc.hadoop.cascading.SubAssemblies.NamingPipe;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.UniqueCountBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.UniqueCountBy.UniqueCountByData;

import cascading.pipe.HashJoin;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.pipe.assembly.SumBy;
import cascading.pipe.joiner.InnerJoin;
import cascading.tuple.Fields;

import com.google.common.collect.ImmutableList;

public class Table1 extends SubAssembly {

  private static final Fields COUNT_BY_FIELDS = PROJECT_ID_FIELD.append(TYPE_FIELD);

  // Temporary fields
  private static final Fields SPECIMEN_COUNT_BY_FIELDS =
      getRedundantFieldCounterpart("specimen_" + PROJECT_ID_FIELD)
          .append(getRedundantFieldCounterpart("specimen_" + TYPE_FIELD));
  private static final Fields SAMPLE_COUNT_BY_FIELDS =
      getRedundantFieldCounterpart("sample_" + PROJECT_ID_FIELD)
          .append(getRedundantFieldCounterpart("sample_" + TYPE_FIELD));
  private static final Fields OBSERVATION_COUNT_BY_FIELDS =
      getRedundantFieldCounterpart("observation_" + PROJECT_ID_FIELD)
          .append(getRedundantFieldCounterpart("observation_" + TYPE_FIELD));

  public Table1(Pipe preComputationTable) {
    setTails(process(preComputationTable));
  }

  private static Pipe process(Pipe preComputationTable) {
    return new Retain(
        new HashJoin(
            // TODO: create more readable subassembly
            toArray(

                ImmutableList.<Pipe> builder()

                    .add(processDonors(preComputationTable))
                    .add(processSpecimens(preComputationTable))
                    .add(processSamples(preComputationTable))
                    .add(processObservations(preComputationTable))

                    .build()
                , Pipe.class),
            new Fields[] { COUNT_BY_FIELDS, SPECIMEN_COUNT_BY_FIELDS, SAMPLE_COUNT_BY_FIELDS, OBSERVATION_COUNT_BY_FIELDS },
            DONOR_UNIQUE_COUNT_FIELD.append(COUNT_BY_FIELDS)
                .append(SPECIMEN_UNIQUE_COUNT_FIELD.append(SPECIMEN_COUNT_BY_FIELDS))
                .append(SAMPLE_UNIQUE_COUNT_FIELD.append(SAMPLE_COUNT_BY_FIELDS))
                .append(_ANALYSIS_OBSERVATION_COUNT_FIELD.append(OBSERVATION_COUNT_BY_FIELDS)),
            new InnerJoin()),
        COUNT_BY_FIELDS
            .append(DONOR_UNIQUE_COUNT_FIELD)
            .append(SPECIMEN_UNIQUE_COUNT_FIELD)
            .append(SAMPLE_UNIQUE_COUNT_FIELD)
            .append(_ANALYSIS_OBSERVATION_COUNT_FIELD));
  }

  /**
   * This one is also used by {@link Table2}.
   */
  public static Pipe processDonors(Pipe preComputationTable) {
    return new NamingPipe(
        DONOR,

        new UniqueCountBy(UniqueCountByData.builder() // No renaming needed here

            .pipe(preComputationTable)
            .uniqueFields(
                COUNT_BY_FIELDS
                    .append(DONOR_ID_FIELD))
            .countByFields(COUNT_BY_FIELDS)
            .resultCountField(getCountFieldCounterpart(DONOR_ID_FIELD))

            .build()));
  }

  private static Pipe processSpecimens(Pipe preComputationTable) {
    return new NamingPipe(
        SPECIMEN,

        new Rename(
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

                .build()),
            COUNT_BY_FIELDS,
            SPECIMEN_COUNT_BY_FIELDS));
  }

  private static Pipe processSamples(Pipe preComputationTable) {
    return new NamingPipe(
        SAMPLE,

        new Rename(
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

                .build()),
            COUNT_BY_FIELDS,
            SAMPLE_COUNT_BY_FIELDS));
  }

  private static Pipe processObservations(Pipe preComputationTable) {
    return new NamingPipe(
        OBSERVATION,

        new Rename(

            new SumBy( // TODO: retain necessary?
                preComputationTable,
                PROJECT_ID_FIELD.append(TYPE_FIELD),
                _ANALYSIS_OBSERVATION_COUNT_FIELD,
                _ANALYSIS_OBSERVATION_COUNT_FIELD,
                long.class),
            COUNT_BY_FIELDS,
            OBSERVATION_COUNT_BY_FIELDS));
  }

}
