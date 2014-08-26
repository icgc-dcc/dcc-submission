package org.icgc.dcc.submission.reporter.cascading.subassembly;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.ARGS;
import static cascading.tuple.Fields.NONE;
import static cascading.tuple.Fields.REPLACE;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.icgc.dcc.core.model.FeatureTypes.hasSequencingStrategy;
import static org.icgc.dcc.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.core.util.Strings2.NOT_APPLICABLE;
import static org.icgc.dcc.hadoop.cascading.Fields2.appendIfApplicable;
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
import org.icgc.dcc.hadoop.cascading.operation.BaseFunction;

import cascading.flow.FlowProcess;
import cascading.operation.FunctionCall;
import cascading.operation.Insert;
import cascading.pipe.Each;
import cascading.pipe.HashJoin;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.CountBy;
import cascading.pipe.assembly.Discard;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.pipe.joiner.InnerJoin;
import cascading.pipe.joiner.RightJoin;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

@Slf4j
public class PreComputation extends SubAssembly {

  private static Fields META_PK_FIELDS = ANALYSIS_ID_FIELD.append(SAMPLE_ID_FIELD);
  private final int NO_OBSERVATIONS_COUNT = 0;

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
    this.featureTypesWithData = newLinkedHashSet( // To make it serializable
        DataTypes.getFeatureTypes(
            FileType.getDataTypes(
                matchingFilePathCounts.keySet())));

    setTails(processProject());
  }

  /**
   * Joins the clinical and observation pipes.
   */
  private Pipe processProject() {

    Pipe featureTypes = processFeatureTypes();
    Pipe clinicalPlain = processClinicalPlain();

    return

    new Each(
        new Each(
            new Each(
                new Join(featureTypes, clinicalPlain),
                NONE.append(TYPE_FIELD)
                    .append(ANALYSIS_ID_FIELD)
                    .append(SEQUENCING_STRATEGY_FIELD)
                    .append(_ANALYSIS_OBSERVATION_COUNT_FIELD),
                new OrphanReplacer(),
                REPLACE),
            new Insert(PROJECT_ID_FIELD, projectKey),
            ALL
        ),
        new Insert(RELEASE_NAME_FIELD, releaseName),
        ALL);
  }

  public static class Join extends SubAssembly {

    private final Pipe featureTypes;
    private final Pipe clinicalPlain;

    Join(Pipe featureTypes, Pipe clinicalPlain) {
      this.featureTypes = featureTypes;
      this.clinicalPlain = clinicalPlain;

      setTails(plainJoin());
    }

    private Pipe plainJoin() {
      return new Discard(
          new HashJoin(
              new Rename(
                  featureTypes,
                  SAMPLE_ID_FIELD,
                  REDUNDANT_SAMPLE_ID_FIELD),
              REDUNDANT_SAMPLE_ID_FIELD,
              clinicalPlain,
              SAMPLE_ID_FIELD,
              new RightJoin()
          ),
          REDUNDANT_SAMPLE_ID_FIELD);
    }

  }

  private Pipe processClinicalPlain() {
    return new Discard(
        new HashJoin(

            processSpecimenFiles(),
            SPECIMEN_ID_FIELD,

            processSampleFiles(),
            SPECIMEN_ID_FIELD,

            DONOR_ID_FIELD
                .append(REDUNDANT_SPECIMEN_ID_FIELD)
                .append(SPECIMEN_ID_FIELD)
                .append(SAMPLE_ID_FIELD),

            new InnerJoin()),
        REDUNDANT_SPECIMEN_ID_FIELD);
  }

  private Pipe processSpecimenFiles() {
    return processFiles(SPECIMEN_TYPE, DONOR_ID_FIELD.append(SPECIMEN_ID_FIELD));
  }

  private Pipe processSampleFiles() {
    return processFiles(SAMPLE_TYPE, SPECIMEN_ID_FIELD.append(SAMPLE_ID_FIELD));
  }

  private Pipe processFeatureTypes() {
    Pipe[] array = new Pipe[newArrayList(featureTypesWithData).size()];
    int i = 0;
    for (val featureType : featureTypesWithData) {
      array[i++] = processFeatureTypePlain(featureType);
    }

    return new Merge(array);
  }

  private Pipe processFeatureTypePlain(@NonNull final FeatureType featureType) {
    log.info("Processing '{}'", featureType);

    return

    // Insert feature type
    new Each(
        new Discard(
            new HashJoin(

                processPrimaryFilesPlain(featureType),
                META_PK_FIELDS,

                processMetaFiles(featureType),
                META_PK_FIELDS,

                META_PK_FIELDS
                    .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)
                    .append(REDUNDANT_ANALYSIS_ID_FIELD)
                    .append(REDUNDANT_SAMPLE_ID_FIELD)
                    .append(SEQUENCING_STRATEGY_FIELD),

                new InnerJoin()),
            REDUNDANT_ANALYSIS_ID_FIELD
                .append(REDUNDANT_SAMPLE_ID_FIELD)),
        new Insert(TYPE_FIELD, getDataTypeValue(featureType)),
        ALL);
  }

  private Pipe processPrimaryFilesPlain(@NonNull final FeatureType featureType) {
    return new CountBy(
        processFiles(
            featureType.getPrimaryFileType(),
            META_PK_FIELDS),
        META_PK_FIELDS,
        _ANALYSIS_OBSERVATION_COUNT_FIELD);
  }

  private Pipe processMetaFiles(@NonNull final FeatureType featureType) {
    return normalizeSequencingStrategies(
        processFiles(
            featureType.getMetaFileType(),
            appendIfApplicable(
                META_PK_FIELDS,
                hasSequencingStrategy(featureType),
                SEQUENCING_STRATEGY_FIELD)),

        // Use feature type as replacement for a sequencing strategy if need be
        getDataTypeValue(featureType),

        featureType.hasSequencingStrategy());
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
  private Pipe normalizeSequencingStrategies(
      @NonNull final Pipe pipe,
      @NonNull final String replacement,
      final boolean hasSequencingStrategy) {
    return hasSequencingStrategy ?
        pipe :

        // Insert a "fake" sequencing strategy to make it look uniform
        new Each(
            pipe,
            new Insert(SEQUENCING_STRATEGY_FIELD, replacement),
            ALL
        );
  }

  /**
   * Must use the {@link String} value because {@link DataType} is not a real enum (rather a composite thereof).
   */
  private String getDataTypeValue(@NonNull final DataType dataType) {
    return dataType.getTypeName();
  }

  private class OrphanReplacer extends BaseFunction<Void> {

    private OrphanReplacer() {
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
