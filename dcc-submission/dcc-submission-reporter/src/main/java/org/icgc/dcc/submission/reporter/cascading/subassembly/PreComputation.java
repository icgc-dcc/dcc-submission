package org.icgc.dcc.submission.reporter.cascading.subassembly;

import static cascading.tuple.Fields.ARGS;
import static cascading.tuple.Fields.NONE;
import static cascading.tuple.Fields.REPLACE;
import static org.icgc.dcc.core.model.FeatureTypes.hasSequencingStrategy;
import static org.icgc.dcc.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.core.util.Strings2.NOT_APPLICABLE;
import static org.icgc.dcc.hadoop.cascading.Fields2.appendIfApplicable;
import static org.icgc.dcc.hadoop.cascading.Fields2.keyValuePair;
import static org.icgc.dcc.submission.reporter.Reporter.ORPHAN_TYPE;
import static org.icgc.dcc.submission.reporter.Reporter.getHeadPipeName;
import static org.icgc.dcc.submission.reporter.ReporterFields.ANALYSIS_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.REDUNDANT_ANALYSIS_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.REDUNDANT_SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.REDUNDANT_SPECIMEN_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.RELEASE_NAME_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SPECIMEN_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.TYPE_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;

import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.core.model.DataType.DataTypes;
import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.CountByData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.HashCountBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Insert;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin.JoinData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Transformerge;
import org.icgc.dcc.hadoop.cascading.operation.BaseFunction;

import cascading.flow.FlowProcess;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.pipe.joiner.InnerJoin;
import cascading.pipe.joiner.LeftJoin;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

@Slf4j
public class PreComputation extends SubAssembly {

  private static final int NO_OBSERVATIONS_COUNT = 0;

  private final String releaseName;
  private final String projectKey;
  private final Iterable<FeatureType> featureTypesWithData;
  private final Map<FileType, Integer> matchingFilePaths;

  public PreComputation(
      @NonNull final String releaseName,
      @NonNull final String projectKey,
      @NonNull final Map<FileType, Integer> matchingFilePathCounts) {
    this.releaseName = releaseName;
    this.projectKey = projectKey;
    this.matchingFilePaths = matchingFilePathCounts;
    this.featureTypesWithData = ImmutableSet.copyOf( // To make it serializable
        DataTypes.getFeatureTypes(
            FileType.getDataTypes(
                matchingFilePathCounts.keySet())));

    setTails(processProject());
  }

  /**
   * Joins the clinical and observation pipes.
   */
  private Pipe processProject() {
    return new Insert(

        // Field/value to be inserted
        keyValuePair(RELEASE_NAME_FIELD, releaseName),

        // Insert project ID
        new Insert(

            // Field/value to be inserted
            keyValuePair(PROJECT_ID_FIELD, projectKey),

            new Each(

                //
                new ReadableHashJoin(
                    JoinData.builder()

                        // Left-join in order to keep track of clinical data with no observations as well
                        .joiner(new LeftJoin())

                        .leftPipe(processClinical())
                        .leftJoinFields(SAMPLE_ID_FIELD)

                        .rightPipe(processFeatureTypes())
                        .rightJoinFields(REDUNDANT_SAMPLE_ID_FIELD)

                        .discardFields(REDUNDANT_SAMPLE_ID_FIELD)

                        .build()),

                // Outer join target fields
                NONE.append(TYPE_FIELD)
                    .append(ANALYSIS_ID_FIELD)
                    .append(SEQUENCING_STRATEGY_FIELD)
                    .append(_ANALYSIS_OBSERVATION_COUNT_FIELD),
                new OuterJoinNullReplacer(),
                REPLACE)));
  }

  private Pipe processClinical() {
    return

    //
    new ReadableHashJoin(
        JoinData.builder()
            .joiner(new InnerJoin())

            .leftPipe(processSpecimenFiles())
            .leftJoinFields(SPECIMEN_ID_FIELD)

            .rightPipe(processSampleFiles())
            .rightJoinFields(SPECIMEN_ID_FIELD)

            .resultFields(
                DONOR_ID_FIELD
                    .append(REDUNDANT_SPECIMEN_ID_FIELD)
                    .append(SPECIMEN_ID_FIELD)
                    .append(SAMPLE_ID_FIELD))
            .discardFields(REDUNDANT_SPECIMEN_ID_FIELD)

            .build());
  }

  private Pipe processSpecimenFiles() {
    return processFiles(SPECIMEN_TYPE, DONOR_ID_FIELD.append(SPECIMEN_ID_FIELD));
  }

  private Pipe processSampleFiles() {
    return processFiles(SAMPLE_TYPE, SPECIMEN_ID_FIELD.append(SAMPLE_ID_FIELD));
  }

  private Pipe processFeatureTypes() {
    return new Rename(new Transformerge<FeatureType>(
        featureTypesWithData,
        new Function<FeatureType, Pipe>() {

          @Override
          public Pipe apply(FeatureType featureType) {
            return processFeatureType(featureType);
          }

        }),
        SAMPLE_ID_FIELD,
        REDUNDANT_SAMPLE_ID_FIELD);
  }

  private Pipe processFeatureType(@NonNull final FeatureType featureType) {
    log.info("Processing '{}'", featureType);

    return

    // Insert feature type
    new Insert(

        // Fields to insert
        keyValuePair(TYPE_FIELD, getDataTypeValue(featureType)),

        //
        new ReadableHashJoin(
            JoinData.builder()

                .joiner(new InnerJoin())

                .leftPipe(processPrimaryFiles(featureType))
                .leftJoinFields(ANALYSIS_ID_FIELD.append(SAMPLE_ID_FIELD))

                .rightPipe(

                    // Meta files
                    normalizeSequencingStrategies(
                        processFiles(featureType.getMetaFileType(),
                            appendIfApplicable(
                                ANALYSIS_ID_FIELD.append(SAMPLE_ID_FIELD),
                                hasSequencingStrategy(featureType),
                                SEQUENCING_STRATEGY_FIELD)),

                        featureType.hasSequencingStrategy(),

                        // Use feature type as replacement for a sequencing strategy if need be
                        getDataTypeValue(featureType)))
                .rightJoinFields(REDUNDANT_ANALYSIS_ID_FIELD.append(REDUNDANT_SAMPLE_ID_FIELD))

                .discardFields(REDUNDANT_ANALYSIS_ID_FIELD.append(REDUNDANT_SAMPLE_ID_FIELD))

                .build()));
  }

  private Pipe processPrimaryFiles(@NonNull final FeatureType featureType) {
    return

    // TODO: move that before the merge to maximum parallelization (optimization)
    new HashCountBy(CountByData.builder()

        .pipe(processFiles(
            featureType.getPrimaryFileType(),
            ANALYSIS_ID_FIELD.append(SAMPLE_ID_FIELD)))
        .countByFields(ANALYSIS_ID_FIELD.append(SAMPLE_ID_FIELD))
        .resultCountField(_ANALYSIS_OBSERVATION_COUNT_FIELD)

        .build());

  }

  private Pipe processFiles(
      @NonNull final FileType fileType,
      @NonNull final Fields retainedFields) {
    val pipes = new Pipe[matchingFilePaths.get(fileType)];
    for (int fileNumber = 0; fileNumber < pipes.length; fileNumber++) {
      pipes[fileNumber] = processFile(fileType, fileNumber, retainedFields);
    }

    return new Merge(pipes);
  }

  private Pipe processFile(
      @NonNull final FileType fileType,
      final int fileNumber,
      @NonNull final Fields retainedFields) {
    return new Retain(
        new Pipe(
            getHeadPipeName(projectKey, fileType, fileNumber)),
        retainedFields);
  }

  /**
   * Some feature types don't have a sequencing strategy and require special processing.
   */
  private static Pipe normalizeSequencingStrategies(
      @NonNull final Pipe pipe,
      final boolean hasSequencingStrategy,
      @NonNull final String replacement) {
    return new Rename(
        hasSequencingStrategy ?
            pipe :

            // Insert a "fake" sequencing strategy to make it look uniform
            new Insert(
                keyValuePair(SEQUENCING_STRATEGY_FIELD, replacement),
                pipe
            ),
        ANALYSIS_ID_FIELD.append(SAMPLE_ID_FIELD),
        REDUNDANT_ANALYSIS_ID_FIELD.append(REDUNDANT_SAMPLE_ID_FIELD));
  }

  /**
   * Must use the {@link String} value because {@link DataType} is not a real enum (rather a composite thereof).
   */
  private static String getDataTypeValue(@NonNull final DataType dataType) {
    return dataType.getId();
  }

  private static class OuterJoinNullReplacer extends BaseFunction<Void> {

    private OuterJoinNullReplacer() {
      super(ARGS);
    }

    @Override
    public void operate(
        @SuppressWarnings("rawtypes") FlowProcess flowProcess,
        FunctionCall<Void> functionCall) {
      functionCall
          .getOutputCollector()
          .add(getUpdatedTuple(functionCall.getArguments()));
    }

    private Tuple getUpdatedTuple(@NonNull final TupleEntry entry) {
      if (isOrphanSample(entry)) {
        return new Tuple(
            ORPHAN_TYPE,
            NOT_APPLICABLE,
            NOT_APPLICABLE,
            NO_OBSERVATIONS_COUNT);
      } else {
        return new Tuple(
            entry.getString(TYPE_FIELD),
            entry.getString(ANALYSIS_ID_FIELD),
            entry.getString(SEQUENCING_STRATEGY_FIELD),
            entry.getInteger(_ANALYSIS_OBSERVATION_COUNT_FIELD));
      }
    }

    private boolean isOrphanSample(@NonNull final TupleEntry entry) {
      val featureType = entry.getString(TYPE_FIELD);
      return featureType == null || featureType.isEmpty();
    }

  }

}
