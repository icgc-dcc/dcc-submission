package org.icgc.dcc.submission.reporter.cascading.subassembly;

import static org.icgc.dcc.common.cascading.Fields2.getCountFieldCounterpart;
import static org.icgc.dcc.common.core.model.FeatureTypes.withSequencingStrategy;
import static org.icgc.dcc.common.core.model.SpecialValue.MISSING_CODES;
import static org.icgc.dcc.submission.reporter.OutputType.SEQUENCING_STRATEGY;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SEQUENCING_STRATEGY_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;
import static org.icgc.dcc.submission.reporter.cascading.subassembly.ProjectDataTypeEntity.donorUniqueCountBy;

import java.util.List;
import java.util.Set;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.common.cascading.SubAssemblies.NullReplacer;
import org.icgc.dcc.common.cascading.SubAssemblies.NullReplacer.NullReplacing;
import org.icgc.dcc.common.cascading.SubAssemblies.ReadableHashJoin;
import org.icgc.dcc.common.cascading.SubAssemblies.ReadableHashJoin.JoinData;
import org.icgc.dcc.common.cascading.SubAssemblies.TransposeBuffer;
import org.icgc.dcc.common.cascading.SubAssemblies.UniqueCountBy;
import org.icgc.dcc.common.cascading.SubAssemblies.UniqueCountBy.UniqueCountByData;
import org.icgc.dcc.common.core.model.FeatureTypes.FeatureType;

import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Discard;
import cascading.tuple.Fields;

import com.google.common.collect.ImmutableList;

public class ProjectSequencingStrategy extends SubAssembly {

  private static final String NULL_REPLACEMENT = "null";
  private static final long TRANSPOSITION_DEFAULT_VALUE = 0L;

  public ProjectSequencingStrategy(
      @NonNull final Pipe preComputationTable,
      @NonNull final String releaseName,
      @NonNull final Set<String> codes) {

    setTails(process(
        preComputationTable,
        getTranspositionFields(codes)));
  }

  private static Pipe process(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields transpositionFields) {
    return new ReadableHashJoin(JoinData.builder()

        .innerJoin()

        .leftPipe(donorUniqueCountBy(preComputationTable, PROJECT_ID_FIELD))
        .rightPipe(processSequencingStrategies(preComputationTable, transpositionFields))

        .joinFields(PROJECT_ID_FIELD)

        .build());
  }

  private static Pipe processSequencingStrategies(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields transpositionFields) {

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
                    new UniqueCountBy(SEQUENCING_STRATEGY.getId(), UniqueCountByData.builder()

                        .pipe(preComputationTable)
                        .uniqueFields(
                            PROJECT_ID_FIELD
                                .append(SEQUENCING_STRATEGY_FIELD)
                                .append(DONOR_ID_FIELD))
                        .countByFields(PROJECT_ID_FIELD.append(SEQUENCING_STRATEGY_FIELD))
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

  private static Fields getTranspositionFields(@NonNull final Set<String> codes) {
    Fields transpositionFields = new Fields();
    for (val code : getAugmentedCodes(codes)) {
      transpositionFields = transpositionFields.append(new Fields(code));
    }
    return transpositionFields;
  }

  private static List<String> getAugmentedCodes(@NonNull final Set<String> codes) {

    val builder = new ImmutableList.Builder<String>();
    val iterator = codes.iterator();
    for (int i = 0; i < codes.size(); i++) {
      builder.add(iterator.next());
    }

    for (val featureType : withSequencingStrategy(FeatureType.values())) {
      builder.add(featureType.getId());
    }

    // Remove this after DCC-2399 is done
    builder.add(NULL_REPLACEMENT);
    builder.addAll(MISSING_CODES);

    return builder.build();
  }

}
