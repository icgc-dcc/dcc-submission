package org.icgc.dcc.submission.reporter.cascading.subassembly;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.ARGS;
import static cascading.tuple.Fields.NONE;
import static cascading.tuple.Fields.REPLACE;
import static org.icgc.dcc.core.model.FeatureTypes.hasSequencingStrategy;
import static org.icgc.dcc.core.model.FieldNames.ReporterFieldNames.RELEASE_NAME;
import static org.icgc.dcc.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.core.util.Strings2.NOT_APPLICABLE;
import static org.icgc.dcc.hadoop.cascading.Fields2.appendIfApplicable;
import static org.icgc.dcc.hadoop.cascading.Fields2.keyValuePair;
import static org.icgc.dcc.submission.reporter.IntermediateOutputType.PRE_COMPUTATION;
import static org.icgc.dcc.submission.reporter.IntermediateOutputType.PRE_COMPUTATION_CLINICAL;
import static org.icgc.dcc.submission.reporter.IntermediateOutputType.PRE_COMPUTATION_FEATURE_TYPES;
import static org.icgc.dcc.submission.reporter.IntermediateOutputType.PRE_COMPUTATION_TMP1;
import static org.icgc.dcc.submission.reporter.IntermediateOutputType.PRE_COMPUTATION_TMP2;
import static org.icgc.dcc.submission.reporter.Reporter.ORPHAN_TYPE;
import static org.icgc.dcc.submission.reporter.Reporter.getHeadPipeName;
import static org.icgc.dcc.submission.reporter.ReporterFields.ANALYSIS_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.REDUNDANT_ANALYSIS_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.REDUNDANT_SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.REDUNDANT_SPECIMEN_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SPECIMEN_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.TYPE_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.cascading.subassembly.projectdatatypeentity.Dumps.addIntermediateOutputDump;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.CountByData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.HashCountBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Insert;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin.JoinData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Transformerge;
import org.icgc.dcc.hadoop.cascading.operation.BaseFunction;
import org.icgc.dcc.submission.reporter.ReporterInput;
import org.icgc.dcc.submission.reporter.cascading.subassembly.projectdatatypeentity.Dumps;

import cascading.flow.FlowProcess;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.HashJoin;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Discard;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.pipe.assembly.Unique;
import cascading.pipe.joiner.InnerJoin;
import cascading.pipe.joiner.RightJoin;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.base.Function;

@Slf4j
public class PreComputation extends SubAssembly {

  private static Fields META_PK_FIELDS = ANALYSIS_ID_FIELD.append(SAMPLE_ID_FIELD);
  private static final int NO_OBSERVATIONS_COUNT = 0;

  public PreComputation(
      @NonNull final String releaseName,
      @NonNull final String projectKey,
      @NonNull final ReporterInput reporterInput) {
    setTails(Dumps.addIntermediateOutputDump(
        PRE_COMPUTATION,
        projectKey,
        processProject(
            reporterInput,
            releaseName,
            projectKey)));
  }

  /**
   * Joins the clinical and observation pipes.
   */
  private static Pipe processProject(
      @NonNull final ReporterInput reporterInput,
      @NonNull final String releaseName,
      @NonNull final String projectKey) {
    Pipe previous2 = foo(reporterInput, projectKey);
    return

    new Insert(

        // Field/value to be inserted
        keyValuePair(RELEASE_NAME, releaseName),

        // Insert project ID
        new Insert(

            // Field/value to be inserted
            keyValuePair(PROJECT_ID_FIELD, projectKey),

            //
            addIntermediateOutputDump(PRE_COMPUTATION_TMP2, projectKey,
                new Unique(
                    new Each(
                        addIntermediateOutputDump(PRE_COMPUTATION_TMP1, projectKey, previous2),
                        NONE.append(TYPE_FIELD)
                            .append(ANALYSIS_ID_FIELD)
                            .append(SEQUENCING_STRATEGY_FIELD)
                            .append(_ANALYSIS_OBSERVATION_COUNT_FIELD),
                        new OrphanReplacer(),
                        REPLACE),
                    ALL))
        ));
  }

  private static Pipe foo2(final ReporterInput reporterInput, final String projectKey) {
    return new Discard(
        new HashJoin(
            addIntermediateOutputDump(PRE_COMPUTATION_FEATURE_TYPES, projectKey,
                new Rename(
                    processFeatureTypes(reporterInput, projectKey),
                    SAMPLE_ID_FIELD,
                    REDUNDANT_SAMPLE_ID_FIELD)),
            REDUNDANT_SAMPLE_ID_FIELD,
            addIntermediateOutputDump(PRE_COMPUTATION_CLINICAL, projectKey,
                processClinical(reporterInput, projectKey)),
            SAMPLE_ID_FIELD,
            new RightJoin()
        ),
        REDUNDANT_SAMPLE_ID_FIELD);
  }

  /**
   * @param reporterInput
   * @param projectKey
   * @return
   */
  private static ReadableHashJoin foo(final ReporterInput reporterInput, final String projectKey) {
    ReadableHashJoin previous2 = new ReadableHashJoin(JoinData.builder()

        // Right-join in order to keep track of clinical data with no observations as well
        .joiner(new RightJoin())

        .leftPipe(
            addIntermediateOutputDump(PRE_COMPUTATION_FEATURE_TYPES, projectKey,
                new Rename(
                    processFeatureTypes(reporterInput, projectKey),
                    SAMPLE_ID_FIELD,
                    REDUNDANT_SAMPLE_ID_FIELD)))
        .leftJoinFields(REDUNDANT_SAMPLE_ID_FIELD)

        .rightPipe(
            addIntermediateOutputDump(PRE_COMPUTATION_CLINICAL, projectKey,
                processClinical(reporterInput, projectKey)))
        .rightJoinFields(SAMPLE_ID_FIELD)

        .resultFields(
            NONE.append(ANALYSIS_ID_FIELD)
                .append(REDUNDANT_SAMPLE_ID_FIELD)
                .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)
                .append(SEQUENCING_STRATEGY_FIELD)
                .append(TYPE_FIELD)
                .append(DONOR_ID_FIELD)
                .append(SPECIMEN_ID_FIELD)
                .append(SAMPLE_ID_FIELD)
        )
        .discardFields(REDUNDANT_SAMPLE_ID_FIELD)

        .build());
    return previous2;
  }

  private static class OrphanReplacer extends BaseFunction<Void> {

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

    private static Tuple getUpdatedTuple(@NonNull final TupleEntry entry) {
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
            entry.getString(_ANALYSIS_OBSERVATION_COUNT_FIELD));
      }
    }

    private static boolean isOrphanSample(@NonNull final TupleEntry entry) {
      val featureType = entry.getString(TYPE_FIELD);
      return featureType == null || featureType.isEmpty();
    }

  }

  private static Pipe processClinical(final ReporterInput reporterInput, String projectKey) {
    return new ReadableHashJoin(
        JoinData.builder()
            .joiner(new InnerJoin())

            .leftPipe(processSpecimenFiles(reporterInput, projectKey))
            .leftJoinFields(SPECIMEN_ID_FIELD)

            .rightPipe(processSampleFiles(reporterInput, projectKey))
            .rightJoinFields(SPECIMEN_ID_FIELD)

            .resultFields(
                DONOR_ID_FIELD
                    .append(REDUNDANT_SPECIMEN_ID_FIELD)
                    .append(SPECIMEN_ID_FIELD)
                    .append(SAMPLE_ID_FIELD))
            .discardFields(REDUNDANT_SPECIMEN_ID_FIELD)

            .build());
  }

  private static Pipe processSpecimenFiles(
      @NonNull final ReporterInput reporterInput,
      @NonNull final String projectKey) {
    return processFiles(reporterInput, projectKey, SPECIMEN_TYPE, DONOR_ID_FIELD.append(SPECIMEN_ID_FIELD));
  }

  private static Pipe processSampleFiles(
      @NonNull final ReporterInput reporterInput,
      @NonNull final String projectKey) {
    return processFiles(reporterInput, projectKey, SAMPLE_TYPE, SPECIMEN_ID_FIELD.append(SAMPLE_ID_FIELD));
  }

  private static Pipe processFeatureTypes(final ReporterInput reporterInput, final String projectKey) {
    return

    //
    new Transformerge<FeatureType>(
        reporterInput.getFeatureTypesWithData(projectKey),
        new Function<FeatureType, Pipe>() {

          @Override
          public Pipe apply(FeatureType featureType) {
            return processFeatureType(reporterInput, projectKey, featureType);
          }

        });
  }

  private static Pipe processFeatureType(
      @NonNull final ReporterInput reporterInput,
      @NonNull final String projectKey,
      @NonNull final FeatureType featureType) {
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

                .leftPipe(processPrimaryFiles(reporterInput, projectKey, featureType))
                .leftJoinFields(META_PK_FIELDS)

                .rightPipe(

                    // Meta files
                    normalizeSequencingStrategies(
                        processFiles(
                            reporterInput, projectKey, featureType.getMetaFileType(),
                            appendIfApplicable(
                                META_PK_FIELDS,
                                hasSequencingStrategy(featureType),
                                SEQUENCING_STRATEGY_FIELD)),

                        // Use feature type as replacement for a sequencing strategy if need be
                        featureType.getTypeName(),

                        featureType.hasSequencingStrategy()))
                .rightJoinFields(META_PK_FIELDS)

                .resultFields(
                    META_PK_FIELDS
                        .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)
                        .append(REDUNDANT_ANALYSIS_ID_FIELD)
                        .append(REDUNDANT_SAMPLE_ID_FIELD)
                        .append(SEQUENCING_STRATEGY_FIELD))
                .discardFields(
                    REDUNDANT_ANALYSIS_ID_FIELD
                        .append(REDUNDANT_SAMPLE_ID_FIELD))

                .build()));
  }

  /**
   * Some feature types don't have a sequencing strategy and require special processing.
   */
  private static Pipe normalizeSequencingStrategies(
      @NonNull final Pipe pipe,
      @NonNull final String replacement,
      final boolean hasSequencingStrategy) {
    return hasSequencingStrategy ?
        pipe :

        // Insert a "fake" sequencing strategy to make it look uniform
        new Insert(
            keyValuePair(SEQUENCING_STRATEGY_FIELD, replacement),
            pipe
        );
  }

  private static Pipe processPrimaryFiles(
      @NonNull final ReporterInput reporterInput,
      @NonNull final String projectKey,
      @NonNull final FeatureType featureType) {
    return

    // TODO: move that before the merge to maximum parallelization (optimization)
    new HashCountBy(CountByData.builder()

        .pipe(processFiles(
            reporterInput, projectKey, featureType.getPrimaryFileType(),
            META_PK_FIELDS))
        .countByFields(META_PK_FIELDS)
        .resultCountField(_ANALYSIS_OBSERVATION_COUNT_FIELD)

        .build());

  }

  private static Pipe processFiles(
      @NonNull final ReporterInput reporterInput,
      @NonNull final String projectKey,
      @NonNull final FileType fileType,
      @NonNull final Fields retainedFields) {
    return new Transformerge<String>(
        reporterInput.getMatchingFilePaths(projectKey, fileType),
        new Function<String, Pipe>() {

          int fileNumber = 0;

          @Override
          public Pipe apply(String matchingFilePath) {
            return processFile(projectKey, fileType, fileNumber++, matchingFilePath, retainedFields);
          }

        });
  }

  private static Pipe processFile(
      @NonNull final String projectKey,
      @NonNull final FileType fileType,
      final int fileNumber,
      @NonNull final String matchingFilePath,
      @NonNull final Fields retainedFields) {
    return new Retain(
        new Pipe(getHeadPipeName(projectKey, fileType, fileNumber)),
        retainedFields);
  }

  /**
   * Must use the {@link String} value because {@link DataType} is not a real enum (rather a composite thereof).
   */
  private static String getDataTypeValue(@NonNull final DataType dataType) {
    return dataType.getTypeName();
  }

}
