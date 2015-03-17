package org.icgc.dcc.submission.reporter.cascading.subassembly;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.ARGS;
import static cascading.tuple.Fields.NONE;
import static cascading.tuple.Fields.REPLACE;
import static org.icgc.dcc.common.cascading.Fields2.appendFieldIfApplicable;
import static org.icgc.dcc.common.cascading.Fields2.getRedundantFieldCounterpart;
import static org.icgc.dcc.common.cascading.Fields2.keyValuePair;
import static org.icgc.dcc.common.core.model.FeatureTypes.hasControlSampleId;
import static org.icgc.dcc.common.core.model.FeatureTypes.hasSequencingStrategy;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.common.core.model.SpecialValue.isMissingCode;
import static org.icgc.dcc.submission.reporter.Reporter.getHeadPipeName;
import static org.icgc.dcc.submission.reporter.ReporterFields.ANALYSIS_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.CONTROL_SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_ID_FIELD;
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
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.cascading.SubAssemblies.CountByData;
import org.icgc.dcc.common.cascading.SubAssemblies.HashCountBy;
import org.icgc.dcc.common.cascading.SubAssemblies.Insert;
import org.icgc.dcc.common.cascading.SubAssemblies.ReadableHashJoin;
import org.icgc.dcc.common.cascading.SubAssemblies.ReadableHashJoin.JoinData;
import org.icgc.dcc.common.cascading.SubAssemblies.ReorderAllFields;
import org.icgc.dcc.common.cascading.SubAssemblies.Transformerge;
import org.icgc.dcc.common.cascading.operation.BaseFunction;
import org.icgc.dcc.common.core.collect.SerializableMaps;
import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.DataType.DataTypes;
import org.icgc.dcc.common.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.common.core.model.FileTypes.FileType;

import cascading.flow.FlowProcess;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Discard;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.base.Function;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.Table;

@Slf4j
public class PreComputation extends SubAssembly {

  private static final String ORPHAN_TYPE = "orphan";

  // TODO: as enum?
  public static final String ORPHAN_SAMPLE_TYPE = ORPHAN_TYPE;
  public static final String CONTROL_SAMPLE_TYPE = "control";
  public static final String TUMOUR_SAMPLE_TYPE = "tumour";

  private static final String ORPHAN_ANALYSIS_ID = ORPHAN_TYPE;

  private static final String ORPHAN_SEQUENCING_STRATEGY = ORPHAN_TYPE;

  // TODO: as enum?
  private static final int ORPHAN_OBSERVATION_COUNT = 0;
  private static final int CONTROL_OBSERVATION_COUNT = 0;

  private final String releaseName;
  private final List<String> projectKeys;
  private final Map<String, Iterable<FeatureType>> featureTypesWithData;
  private final Table<String, FileType, Integer> matchingFilePathCounts;

  public PreComputation(
      @NonNull final String releaseName,
      @NonNull final Set<String> projectKeys,
      @NonNull final Table<String, FileType, Integer> matchingFilePathCounts) {
    this.releaseName = releaseName;
    this.projectKeys = ImmutableList.copyOf(projectKeys);
    this.matchingFilePathCounts = matchingFilePathCounts;
    this.featureTypesWithData = deriveFeatureTypesWithData(matchingFilePathCounts);

    setTails(processRelease());
  }

  private final Map<String, Iterable<FeatureType>> deriveFeatureTypesWithData(
      @NonNull final Table<String, FileType, Integer> matchingFilePathCounts) {
    return SerializableMaps.transformValues(
        matchingFilePathCounts.rowMap(),
        new Function<Map<FileType, Integer>, Iterable<FeatureType>>() {

          @Override
          public Iterable<FeatureType> apply(Map<FileType, Integer> from) {
            return ImmutableSet.copyOf(
                DataTypes.getFeatureTypes(
                    FileType.getDataTypes(
                        from.keySet())));
          }

        });
  }

  /**
   * Joins the clinical and observation pipes.
   */
  private Pipe processRelease() {
    return

    new ReorderAllFields(
        new Insert(

            // Field/value to be inserted
            keyValuePair(RELEASE_NAME_FIELD, releaseName),

            processProjects()),

        // Reordered fields
        NONE.append(RELEASE_NAME_FIELD)
            .append(PROJECT_ID_FIELD)
            .append(DONOR_ID_FIELD)
            .append(SPECIMEN_ID_FIELD)
            .append(SAMPLE_ID_FIELD)
            .append(TYPE_FIELD)
            .append(SAMPLE_TYPE_FIELD)
            .append(ANALYSIS_ID_FIELD)
            .append(SEQUENCING_STRATEGY_FIELD)
            .append(_ANALYSIS_OBSERVATION_COUNT_FIELD));
  }

  private Pipe processProjects() {
    return new Transformerge<String>(
        projectKeys,
        new com.google.common.base.Function<String, Pipe>() {

          @Override
          public Pipe apply(@NonNull final String projectKey) {
            return processProject(projectKey);
          }

        });
  }

  private Pipe processProject(@NonNull final String projectKey) {
    return
    // Insert project ID
    new Insert(

        // Field/value to be inserted
        keyValuePair(PROJECT_ID_FIELD, projectKey),

        new Each(
            new ReadableHashJoin(JoinData.builder()

                // Left-join in order to keep track of clinical data with no observations as well
                .leftJoin()

                .leftPipe(processClinical(projectKey))
                .leftJoinFields(SAMPLE_ID_FIELD)

                .rightPipe(processFeatureTypes(projectKey))
                .rightJoinFields(TUMOUR_SAMPLE_ID_FIELD)

                .build()),
            // Outer join target fields
            NONE.append(TYPE_FIELD)
                .append(SAMPLE_TYPE_FIELD)
                .append(ANALYSIS_ID_FIELD)
                .append(SEQUENCING_STRATEGY_FIELD)
                .append(_ANALYSIS_OBSERVATION_COUNT_FIELD),
            new OuterJoinOrphanReplacer(),
            REPLACE));
  }

  private Pipe processClinical(@NonNull final String projectKey) {
    return new ReadableHashJoin(JoinData.builder()

        .innerJoin()

        .leftPipe(processSpecimenFiles(projectKey))
        .rightPipe(processSampleFiles(projectKey))

        .joinFields(SPECIMEN_ID_FIELD)

        .build());
  }

  private Pipe processSpecimenFiles(@NonNull final String projectKey) {
    return processFiles(projectKey, SPECIMEN_TYPE, DONOR_ID_FIELD.append(SPECIMEN_ID_FIELD));
  }

  private Pipe processSampleFiles(@NonNull final String projectKey) {
    return processFiles(projectKey, SAMPLE_TYPE, SPECIMEN_ID_FIELD.append(SAMPLE_ID_FIELD));
  }

  private Pipe processFeatureTypes(@NonNull final String projectKey) {
    return new Transformerge<FeatureType>(
        featureTypesWithData.get(projectKey),
        new Function<FeatureType, Pipe>() {

          @Override
          public Pipe apply(FeatureType featureType) {
            return new ReorderAllFields( // TODO: move to Transformerge directly
                processFeatureType(projectKey, featureType),
                NONE.append(_ANALYSIS_OBSERVATION_COUNT_FIELD)
                    .append(ANALYSIS_ID_FIELD)
                    .append(SAMPLE_ID_FIELD)
                    .append(SAMPLE_TYPE_FIELD)
                    .append(SEQUENCING_STRATEGY_FIELD)
                    .append(TYPE_FIELD));
          }

        });
  }

  private Pipe processFeatureType(
      @NonNull final String projectKey,
      @NonNull final FeatureType featureType) {
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

                .leftPipe(processMetaFiles(projectKey, featureType))
                .rightPipe(processPrimaryFiles(projectKey, featureType))

                .joinFields(ANALYSIS_ID_FIELD.append(TUMOUR_SAMPLE_ID_FIELD))

                .build()),

            // Outer join target fields
            SAMPLE_TYPE_FIELD.append(_ANALYSIS_OBSERVATION_COUNT_FIELD),
            new OuterJoinControlReplacer(),
            REPLACE));
  }

  private Pipe processMetaFiles(
      @NonNull final String projectKey,
      @NonNull final FeatureType featureType) {
    val metaFiles = normalizeSequencingStrategies(
        processFiles(
            projectKey,
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
        pivotMetaSamples(metaFiles) :
        new Insert(
            keyValuePair(SAMPLE_TYPE_FIELD, TUMOUR_SAMPLE_TYPE),
            metaFiles);
  }

  private Pipe processPrimaryFiles(
      @NonNull final String projectKey,
      @NonNull final FeatureType featureType) {
    // TODO: move that before the merge to maximum parallelization (optimization)
    return new HashCountBy(CountByData.builder()

        .pipe(processFiles(
            projectKey,
            featureType.getPrimaryFileType(),
            ANALYSIS_ID_FIELD.append(TUMOUR_SAMPLE_ID_FIELD)))
        .countByFields(ANALYSIS_ID_FIELD.append(TUMOUR_SAMPLE_ID_FIELD))
        .resultCountField(_ANALYSIS_OBSERVATION_COUNT_FIELD)

        .build());
  }

  /**
   * TODO: create sub-assembly for this pattern (cascading can't do a partial replace)
   */
  private static Pipe pivotMetaSamples(@NonNull final Pipe pipe) {
    val inputFields = TUMOUR_SAMPLE_ID_FIELD.append(CONTROL_SAMPLE_ID_FIELD);
    val temporaryResultFields = getRedundantFieldCounterpart(TUMOUR_SAMPLE_ID_FIELD).append(SAMPLE_TYPE_FIELD);
    val resultFields = TUMOUR_SAMPLE_ID_FIELD.append(SAMPLE_TYPE_FIELD);

    return new Rename(
        new Discard(
            pivot(pipe, inputFields, temporaryResultFields),
            inputFields),
        temporaryResultFields,
        resultFields);
  }

  private static Pipe pivot(
      @NonNull final Pipe pipe,
      @NonNull final Fields inputFields,
      @NonNull final Fields temporaryResultFields) {
    return new Each(
        pipe,
        inputFields,
        new BaseFunction<Void>(
            inputFields.size(),
            temporaryResultFields) {

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
        ALL);
  }

  private Pipe processFiles(
      @NonNull final String projectKey,
      @NonNull final FileType fileType,
      @NonNull final Fields retainedFields) {
    return new Transformerge<Integer>(
        getRange(matchingFilePathCounts.get(projectKey, fileType)),
        new Function<Integer, Pipe>() {

          @Override
          public Pipe apply(@NonNull final Integer fileNumber) {
            return new Retain(
                new Pipe(
                    getHeadPipeName(projectKey, fileType, fileNumber)),
                retainedFields);
          }

        });
  }

  private static Iterable<Integer> getRange(Integer total) {
    return ContiguousSet.create(
        Range.closedOpen(0, total),
        DiscreteDomain.integers()).asList();
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
        new Insert(
            keyValuePair(SEQUENCING_STRATEGY_FIELD, replacement),
            pipe);
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
            CONTROL_OBSERVATION_COUNT);
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
            ORPHAN_SAMPLE_TYPE,
            ORPHAN_ANALYSIS_ID,
            ORPHAN_SEQUENCING_STRATEGY,
            ORPHAN_OBSERVATION_COUNT);
      } else {
        return new Tuple(
            entry.getString(TYPE_FIELD),
            entry.getString(SAMPLE_TYPE_FIELD),
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
