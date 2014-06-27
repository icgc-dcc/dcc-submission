package org.icgc.dcc.reporter;

import static org.icgc.dcc.hadoop.cascading.Fields2.getCountFieldCounterpart;
import static org.icgc.dcc.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.DONOR_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.REDUNDANT_PROJECT_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SEQUENCING_STRATEGY_COUNT_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.TRANSPOSITION_FIELDS;

import org.icgc.dcc.hadoop.cascading.SubAssemblies.NullReplacer;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.NullReplacer.NullReplacing;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin.JoinData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.TransposeBuffer;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.UniqueCountBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.UniqueCountBy.UniqueCountByData;

import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Discard;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.pipe.assembly.SumBy;

public class Table2 extends SubAssembly {

  private static final String NULL_REPLACEMENT = "null";
  private static final long TRANSPOSITION_DEFAULT_VALUE = 0L;

  Table2(Pipe preComputationTable, Pipe donors) {
    setTails(table2(preComputationTable, donors));
  }

  private static Pipe table2(Pipe preComputationTable, Pipe donors) {

    return new ReadableHashJoin(JoinData.builder()

        .leftPipe(postProcessDonors(donors))
        .leftJoinFields(REDUNDANT_PROJECT_ID_FIELD)

        .rightPipe(processSequencingStrategies(preComputationTable))
        .rightJoinFields(PROJECT_ID_FIELD)

        .discardFields(REDUNDANT_PROJECT_ID_FIELD)

        .build());
  }

  private static Pipe postProcessDonors(Pipe pipe) {

    return new Rename(
        new SumBy(
            new Retain(
                pipe,
                PROJECT_ID_FIELD.append(DONOR_UNIQUE_COUNT_FIELD)),
            PROJECT_ID_FIELD,
            DONOR_UNIQUE_COUNT_FIELD,
            DONOR_UNIQUE_COUNT_FIELD,
            long.class),
        PROJECT_ID_FIELD,
        REDUNDANT_PROJECT_ID_FIELD);
  }

  private static Pipe processSequencingStrategies(Pipe preComputationTable) {

    return new Discard(
        new Every(
            new GroupBy(
                new NullReplacer<String>(
                    SEQUENCING_STRATEGY_FIELD,
                    new NullReplacing<String>() {

                      @Override
                      public String get() {
                        return NULL_REPLACEMENT;
                      }

                    },
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

                        .build())),
                PROJECT_ID_FIELD, SEQUENCING_STRATEGY_FIELD),
            new TransposeBuffer<Long>(
                TRANSPOSITION_FIELDS,
                SEQUENCING_STRATEGY_FIELD,
                SEQUENCING_STRATEGY_COUNT_FIELD,
                TRANSPOSITION_DEFAULT_VALUE)),
        SEQUENCING_STRATEGY_FIELD
            .append(SEQUENCING_STRATEGY_COUNT_FIELD));
  }

}
