package org.icgc.dcc.reporter.cascading.subassembly;

import static org.icgc.dcc.core.model.FeatureTypes.TYPES_WITH_SEQUENCING_STRATEGY;
import static org.icgc.dcc.core.model.MissingCodes.MISSING_CODE1;
import static org.icgc.dcc.core.model.MissingCodes.MISSING_CODE2;
import static org.icgc.dcc.hadoop.cascading.Fields2.getCountFieldCounterpart;
import static org.icgc.dcc.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.DONOR_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.REDUNDANT_PROJECT_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SEQUENCING_STRATEGY_COUNT_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;

import java.util.List;
import java.util.Set;

import lombok.NonNull;
import lombok.val;

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
import cascading.tuple.Fields;

import com.google.common.collect.ImmutableList;

public class Table2 extends SubAssembly {

  private static final String NULL_REPLACEMENT = "null";
  private static final long TRANSPOSITION_DEFAULT_VALUE = 0L;

  public Table2(Pipe preComputationTable, Pipe donors, Set<String> codes) {
    setTails(table2(preComputationTable, donors, getTranspositionFields(codes)));
  }

  private static Fields getTranspositionFields(Set<String> codes) {
    Fields transpositionFields = new Fields();
    for (val code : getAugmentedCodes(codes)) {
      transpositionFields = transpositionFields.append(new Fields(code));
    }
    return transpositionFields;
  }

  private static List<String> getAugmentedCodes(
      @NonNull final Set<String> codes) {

    val builder = new ImmutableList.Builder<String>();
    val iterator = codes.iterator();
    for (int i = 0; i < codes.size(); i++) {
      builder.add(iterator.next());
    }

    for (val featureType : TYPES_WITH_SEQUENCING_STRATEGY) {
      builder.add(featureType.getTypeName());
    }

    // Remove this after DCC-2399 is done
    builder.add(NULL_REPLACEMENT);
    builder.add(MISSING_CODE1);
    builder.add(MISSING_CODE2);

    return builder.build();
  }

  private static Pipe table2(Pipe preComputationTable, Pipe donors, Fields transpositionFields) {

    return new ReadableHashJoin(JoinData.builder()

        .leftPipe(postProcessDonors(donors))
        .leftJoinFields(REDUNDANT_PROJECT_ID_FIELD)

        .rightPipe(processSequencingStrategies(preComputationTable, transpositionFields))
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

  private static Pipe processSequencingStrategies(Pipe preComputationTable, Fields transpositionFields) {

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
                transpositionFields,
                SEQUENCING_STRATEGY_FIELD,
                SEQUENCING_STRATEGY_COUNT_FIELD,
                TRANSPOSITION_DEFAULT_VALUE)),
        SEQUENCING_STRATEGY_FIELD
            .append(SEQUENCING_STRATEGY_COUNT_FIELD));
  }

}
