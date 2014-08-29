package org.icgc.dcc.submission.reporter.cascading.subassembly;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.ARGS;
import static cascading.tuple.Fields.NONE;
import static cascading.tuple.Fields.REPLACE;
import static org.icgc.dcc.core.model.FeatureTypes.hasControlSampleId;
import static org.icgc.dcc.core.model.FeatureTypes.hasSequencingStrategy;
import static org.icgc.dcc.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.core.model.SpecialValue.isMissingCode;
import static org.icgc.dcc.core.util.Strings2.NOT_APPLICABLE;
import static org.icgc.dcc.hadoop.cascading.Fields2.appendFieldIfApplicable;
import static org.icgc.dcc.hadoop.cascading.Fields2.getRedundantFieldCounterpart;
import static org.icgc.dcc.hadoop.cascading.Fields2.keyValuePair;
import static org.icgc.dcc.submission.reporter.Reporter.ORPHAN_TYPE;
import static org.icgc.dcc.submission.reporter.Reporter.getHeadPipeName;
import static org.icgc.dcc.submission.reporter.ReporterFields.ANALYSIS_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.CONTROL_SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.FEATURE_TYPE_COMBINED_FIELDS;
import static org.icgc.dcc.submission.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.RELEASE_NAME_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_TYPE_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SPECIMEN_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.TUMOUR_SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.TYPE_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;

import java.util.List;
import java.util.Set;

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
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReorderAllFields;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.SwapFields;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Transformerge;
import org.icgc.dcc.hadoop.cascading.operation.BaseFunction;

import cascading.flow.FlowProcess;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Discard;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;

@Slf4j
public class PreComputation extends SubAssembly {

  private static final String CONTROL_SAMPLE_TYPE = "control";
  private static final String TUMOUR_SAMPLE_TYPE = "tumour";
  private static final int NO_OBSERVATIONS_COUNT = 0;

  private final String releaseName;
  private final List<String> projectKeys;
  private final Iterable<FeatureType> featureTypesWithData;
  private final Table<String, FileType, Integer> matchingFilePathCounts;

  public PreComputation(
      @NonNull final String releaseName,
      @NonNull final Set<String> projectKeys,
      @NonNull final Table<String, FileType, Integer> matchingFilePathCounts) {
    this.releaseName = releaseName;
    this.projectKeys = ImmutableList.copyOf(projectKeys);
    this.matchingFilePathCounts = matchingFilePathCounts;
    this.featureTypesWithData = ImmutableSet.copyOf( // To make it serializable
        DataTypes.getFeatureTypes(
            FileType.getDataTypes(
                matchingFilePathCounts
                    .columnKeySet())));

    setTails(processProject());
  }

  /**
   * Joins the clinical and observation pipes.
   */
  private Pipe processProject() {
    return new Insert(

        // Field/value to be inserted
        keyValuePair(RELEASE_NAME_FIELD, releaseName),

        new Each(
            new ReadableHashJoin(JoinData.builder()

                // Left-join in order to keep track of clinical data with no observations as well
                .leftJoin()

                .leftPipe(processClinical())
                .leftJoinFields(PROJECT_ID_FIELD.append(SAMPLE_ID_FIELD))

                .rightPipe(processFeatureTypes())
                .rightJoinFields(PROJECT_ID_FIELD.append(TUMOUR_SAMPLE_ID_FIELD))

                .build()),
            // Outer join target fields
            NONE.append(TYPE_FIELD)
                .append(ANALYSIS_ID_FIELD)
                .append(SEQUENCING_STRATEGY_FIELD)
                .append(_ANALYSIS_OBSERVATION_COUNT_FIELD),
            new OuterJoinOrphanReplacer(),
            REPLACE));
  }

  private Pipe processClinical() {
    return new ReadableHashJoin(JoinData.builder()

        .innerJoin()

        .leftPipe(processSpecimenFiles())
        .rightPipe(processSampleFiles())

        .joinFields(PROJECT_ID_FIELD.append(SPECIMEN_ID_FIELD))

        .build());
  }

  private Pipe processSpecimenFiles() {
    return processFiles(SPECIMEN_TYPE, DONOR_ID_FIELD.append(SPECIMEN_ID_FIELD));
  }

  private Pipe processSampleFiles() {
    return processFiles(SAMPLE_TYPE, SPECIMEN_ID_FIELD.append(SAMPLE_ID_FIELD));
  }

  private Pipe processFeatureTypes() {
    return new Transformerge<FeatureType>(
        featureTypesWithData,
        new com.google.common.base.Function<FeatureType, Pipe>() {

          @Override
          public Pipe apply(FeatureType featureType) {
            return new ReorderAllFields( // TODO: move to Transformerge directly
                processFeatureType(featureType),
                FEATURE_TYPE_COMBINED_FIELDS);
          }

        });
  }

  private Pipe processFeatureType(@NonNull final FeatureType featureType) {
    log.info("Processing '{}'", featureType);

    return

    // Insert feature type
    new Insert(

        // Fields to insert
        keyValuePair(TYPE_FIELD, getDataTypeValue(featureType)),

        new Each(
            new ReadableHashJoin(JoinData.builder()

                // Left join in order to keep the control sample ID
                .leftJoin()

                .leftPipe(processMetaFiles(featureType))
                .rightPipe(processPrimaryFiles(featureType))

                .joinFields(PROJECT_ID_FIELD.append(ANALYSIS_ID_FIELD).append(TUMOUR_SAMPLE_ID_FIELD))

                .build()),

            // Outer join target fields
            SAMPLE_TYPE_FIELD.append(_ANALYSIS_OBSERVATION_COUNT_FIELD),
            new OuterJoinControlReplacer(),
            REPLACE));
  }

  private Pipe processMetaFiles(@NonNull final FeatureType featureType) {
    val metaFiles = normalizeSequencingStrategies(
        processFiles(
            featureType.getMetaFileType(),

            // Retained fields
            appendFieldIfApplicable(
                appendFieldIfApplicable(
                    ANALYSIS_ID_FIELD.append(TUMOUR_SAMPLE_ID_FIELD),
                    hasControlSampleId(featureType),
                    CONTROL_SAMPLE_ID_FIELD),
                hasSequencingStrategy(featureType),
                SEQUENCING_STRATEGY_FIELD)),

        featureType.hasSequencingStrategy(),

        // Use feature type as replacement for a sequencing strategy if need be
        getDataTypeValue(featureType));

    return hasControlSampleId(featureType).evaluate() ?
        pivotSamples(metaFiles) :
        new Insert(
            keyValuePair(SAMPLE_TYPE_FIELD, TUMOUR_SAMPLE_TYPE),
            metaFiles);
  }

  private Pipe processPrimaryFiles(@NonNull final FeatureType featureType) {
    // TODO: move that before the merge to maximum parallelization (optimization)
    return new HashCountBy(CountByData.builder()

        .pipe(processFiles(
            featureType.getPrimaryFileType(),
            ANALYSIS_ID_FIELD.append(TUMOUR_SAMPLE_ID_FIELD)))
        .countByFields(PROJECT_ID_FIELD.append(ANALYSIS_ID_FIELD).append(TUMOUR_SAMPLE_ID_FIELD))
        .resultCountField(_ANALYSIS_OBSERVATION_COUNT_FIELD)

        .build());
  }

  private Pipe pivotSamples(@NonNull final Pipe pipe) {
    return new Rename( // TODO: create sub-assembly for this pattern
        new Discard(
            new Each(
                pipe,
                TUMOUR_SAMPLE_ID_FIELD.append(CONTROL_SAMPLE_ID_FIELD),
                new BaseFunction<Void>(
                    2,
                    getRedundantFieldCounterpart(TUMOUR_SAMPLE_ID_FIELD).append(SAMPLE_TYPE_FIELD)) {

                  @Override
                  public void operate(
                      @SuppressWarnings("rawtypes") FlowProcess flowProcess,
                      FunctionCall<Void> functionCall) {
                    val entry = functionCall.getArguments();
                    val outputCollector = functionCall.getOutputCollector();

                    val controlSampleId = entry.getString(CONTROL_SAMPLE_ID_FIELD);
                    outputCollector.add(new Tuple(
                        entry.getString(TUMOUR_SAMPLE_ID_FIELD),
                        TUMOUR_SAMPLE_TYPE));

                    if (!isMissingCode(controlSampleId)) {
                      outputCollector.add(new Tuple(
                          controlSampleId,
                          CONTROL_SAMPLE_TYPE));
                    }
                  }

                },
                ALL),
            TUMOUR_SAMPLE_ID_FIELD.append(CONTROL_SAMPLE_ID_FIELD)),
        getRedundantFieldCounterpart(TUMOUR_SAMPLE_ID_FIELD),
        TUMOUR_SAMPLE_ID_FIELD);
  }

  private Pipe processFiles(
      @NonNull final FileType fileType,
      @NonNull final Fields retainedFields) {
    val projectPipes = new Pipe[projectKeys.size()];
    for (int projectNumber = 0; projectNumber < projectPipes.length; projectNumber++) {
      val projectKey = projectKeys.get(projectNumber);

      val filePipes = new Pipe[matchingFilePathCounts.get(projectKey, fileType)];
      for (int fileNumber = 0; fileNumber < filePipes.length; fileNumber++) {
        filePipes[fileNumber] = new Retain(
            new Pipe(
                getHeadPipeName(projectKey, fileType, fileNumber)),
            retainedFields);
      }

      projectPipes[projectNumber] =

          // Insert project ID
          new Insert(

              // Field/value to be inserted
              keyValuePair(PROJECT_ID_FIELD, projectKey),

              new Merge(filePipes));
    }

    return new Merge(projectPipes);
  }

  /**
   * Some feature types don't have a sequencing strategy and require special processing.
   */
  private static Pipe normalizeSequencingStrategies(
      @NonNull final Pipe pipe,
      final boolean hasSequencingStrategy,
      @NonNull final String replacement) {
    return hasSequencingStrategy ?
        pipe :

        // Insert a "fake" sequencing strategy to make it look uniform
        new SwapFields(
            new Insert(
                keyValuePair(SEQUENCING_STRATEGY_FIELD, replacement),
                pipe),
            PROJECT_ID_FIELD.append(SEQUENCING_STRATEGY_FIELD));
  }

  /**
   * Must use the {@link String} value because {@link DataType} is not a real enum (rather a composite thereof).
   */
  private static String getDataTypeValue(@NonNull final DataType dataType) {
    return dataType.getId();
  }

  private static class OuterJoinControlReplacer extends BaseFunction<Void> {

    private OuterJoinControlReplacer() {
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
      if (isControlSample(entry)) {
        return new Tuple(
            CONTROL_SAMPLE_TYPE,
            NO_OBSERVATIONS_COUNT);
      } else {
        return new Tuple(
            entry.getString(SAMPLE_TYPE_FIELD),
            entry.getInteger(_ANALYSIS_OBSERVATION_COUNT_FIELD));
      }
    }

    private boolean isControlSample(@NonNull final TupleEntry entry) {
      return CONTROL_SAMPLE_TYPE.equals(entry.getString(SAMPLE_TYPE_FIELD));
    }

  }

  private static class OuterJoinOrphanReplacer extends BaseFunction<Void> {

    private OuterJoinOrphanReplacer() {
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
