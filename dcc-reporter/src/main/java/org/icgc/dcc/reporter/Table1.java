package org.icgc.dcc.reporter;

import static com.google.common.collect.Iterables.toArray;
import static org.icgc.dcc.hadoop.cascading.Fields2.getCountFieldCounterpart;
import static org.icgc.dcc.reporter.OutputType.DONOR;
import static org.icgc.dcc.reporter.OutputType.OBSERVATION;
import static org.icgc.dcc.reporter.OutputType.SAMPLE;
import static org.icgc.dcc.reporter.OutputType.SPECIMEN;
import static org.icgc.dcc.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SAMPLE_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SPECIMEN_ID_FIELD;
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

  private static final Fields DONOR_COUNT_FIELD = getCountFieldCounterpart(DONOR_ID_FIELD);
  private static final Fields SPECIMEN_COUNT_FIELD = getCountFieldCounterpart(SPECIMEN_ID_FIELD);
  private static final Fields SAMPLE_COUNT_FIELD = getCountFieldCounterpart(SAMPLE_ID_FIELD);

  private static final Fields COUNT_BY_FIELDS = PROJECT_ID_FIELD.append(TYPE_FIELD);
  private static final Fields SP = new Fields("spP", "spT");
  private static final Fields SA = new Fields("saP", "saT");
  private static final Fields OBS = new Fields("oP", "oT");

  Table1(Pipe preComputationTable) {
    setTails(process(preComputationTable));
  }

  private static Pipe process(Pipe preComputationTable) {
    // TODO: inner or side join??
    return new Retain(
        new HashJoin( // TODO: create more readable subassembly
            toArray(

                ImmutableList.<Pipe> builder()

                    .add(processDonors(preComputationTable))
                    .add(processSpecimens(preComputationTable))
                    .add(processSamples(preComputationTable))
                    .add(processObservations(preComputationTable))

                    .build()
                , Pipe.class),
            new Fields[] { COUNT_BY_FIELDS, SP, SA, OBS },
            DONOR_COUNT_FIELD.append(COUNT_BY_FIELDS)
                .append(SPECIMEN_COUNT_FIELD.append(SP))
                .append(SAMPLE_COUNT_FIELD.append(SA))
                .append(_ANALYSIS_OBSERVATION_COUNT_FIELD.append(OBS)),
            new InnerJoin()),
        COUNT_BY_FIELDS
            .append(DONOR_COUNT_FIELD)
            .append(SPECIMEN_COUNT_FIELD)
            .append(SAMPLE_COUNT_FIELD)
            .append(_ANALYSIS_OBSERVATION_COUNT_FIELD));
  }

  static Pipe processDonors(Pipe preComputationTable) {
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
            SP));
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
            SA));
  }

  private static Pipe processObservations(Pipe preComputationTable) {
    return new NamingPipe(
        OBSERVATION,

        new Rename(

            new SumBy(// TODO: retain necessary?
                preComputationTable,
                PROJECT_ID_FIELD.append(TYPE_FIELD),
                _ANALYSIS_OBSERVATION_COUNT_FIELD,
                _ANALYSIS_OBSERVATION_COUNT_FIELD, // TODO: can reuse same?
                long.class),
            COUNT_BY_FIELDS,
            OBS));
  }

}
