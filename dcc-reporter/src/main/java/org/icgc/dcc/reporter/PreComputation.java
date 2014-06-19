package org.icgc.dcc.reporter;

import static org.icgc.dcc.core.model.ClinicalType.CLINICAL_CORE_TYPE;
import static org.icgc.dcc.core.model.FeatureTypes.hasSequencingStrategy;
import static org.icgc.dcc.core.model.FieldNames.ReporterFieldNames.RELEASE_NAME;
import static org.icgc.dcc.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.hadoop.cascading.Fields2.appendIfApplicable;
import static org.icgc.dcc.hadoop.cascading.Fields2.getRedundantFieldCounterpart;
import static org.icgc.dcc.hadoop.cascading.Fields2.keyValuePair;
import static org.icgc.dcc.reporter.Reporter.getHeadPipeName;
import static org.icgc.dcc.reporter.ReporterFields.ANALYSIS_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SAMPLE_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SPECIMEN_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.TYPE_FIELD;
import static org.icgc.dcc.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.CountBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.CountBy.CountByData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.GroupBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.GroupBy.GroupByData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Insert;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.NullReplacer;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.NullReplacer.NullReplacing;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin.JoinData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Transformerge;

import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Retain;
import cascading.pipe.joiner.InnerJoin;
import cascading.pipe.joiner.RightJoin;
import cascading.tuple.Fields;

import com.google.common.base.Function;

@Slf4j
public class PreComputation extends SubAssembly {

  private static Fields REDUNDANT_SPECIMEN_ID_FIELD = getRedundantFieldCounterpart(SPECIMEN_ID_FIELD);
  private static Fields REDUNDANT_SAMPLE_ID_FIELD = getRedundantFieldCounterpart(SAMPLE_ID_FIELD);
  private static Fields REDUNDANT_ANALYSIS_ID_FIELD = getRedundantFieldCounterpart(ANALYSIS_ID_FIELD);

  private static Fields META_PK_FIELDS =
      ANALYSIS_ID_FIELD
          .append(SAMPLE_ID_FIELD);

  // private static String NOT_APPLICABLE = "N/A";

  PreComputation(String releaseName, InputData inputData) {
    setTails(

    // new TupleEntriesLogger(
    // Optional.of(UNIX_NEW_LINE),

    process(releaseName, inputData)

    // )
    ); // TODO: add if?
  }

  private static Pipe process(final String releaseName, final InputData inputData) {
    return

    // Insert release name
    new Insert(

        // Field/value to be inserted
        keyValuePair(RELEASE_NAME, releaseName),

        //
        new Transformerge<String>(
            inputData.getProjectKeys(),
            new Function<String, Pipe>() {

              @Override
              public Pipe apply(String projectKey) {
                return processProject(inputData, projectKey);
              }

            }));
  }

  private static Pipe processProject(final InputData inputData, final String projectKey) {
    return

    new NullReplacer(
        TYPE_FIELD,
        new NullReplacing() {

          @Override
          public Object get() {
            return getValue(CLINICAL_CORE_TYPE);
          }

        },

        // Insert project ID
        new Insert(

            // Field/value to be inserted
            keyValuePair(PROJECT_ID_FIELD, projectKey),

            //
            new ReadableHashJoin(
                JoinData.builder()

                    // Right-join in order to keep track of clinical data with no observations as well
                    .joiner(new RightJoin())

                    .leftPipe(processFeatureTypes(inputData, projectKey))
                    .leftJoinFields(SAMPLE_ID_FIELD)

                    .rightPipe(processClinical(inputData, projectKey))
                    .rightJoinFields(SAMPLE_ID_FIELD)

                    .resultFields(
                        META_PK_FIELDS
                            .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)
                            .append(SEQUENCING_STRATEGY_FIELD)
                            .append(TYPE_FIELD)
                            .append(DONOR_ID_FIELD)
                            .append(SPECIMEN_ID_FIELD)
                            .append(REDUNDANT_SAMPLE_ID_FIELD))
                    .discardFields(REDUNDANT_SAMPLE_ID_FIELD)

                    .build())));
  }

  private static Pipe processClinical(final InputData inputData, String projectKey) {
    return

    //
    new ReadableHashJoin(
        JoinData.builder()
            .joiner(new InnerJoin())

            .leftPipe(
                processFiles(
                    inputData, projectKey, SPECIMEN_TYPE,
                    DONOR_ID_FIELD
                        .append(SPECIMEN_ID_FIELD)))
            .leftJoinFields(SPECIMEN_ID_FIELD)

            .rightPipe(
                processFiles(
                    inputData, projectKey, SAMPLE_TYPE,
                    SPECIMEN_ID_FIELD
                        .append(SAMPLE_ID_FIELD)))
            .rightJoinFields(SPECIMEN_ID_FIELD)

            .resultFields(
                DONOR_ID_FIELD
                    .append(REDUNDANT_SPECIMEN_ID_FIELD)
                    .append(SPECIMEN_ID_FIELD)
                    .append(SAMPLE_ID_FIELD))
            .discardFields(REDUNDANT_SPECIMEN_ID_FIELD)

            .build());
  }

  private static Pipe processFeatureTypes(final InputData inputData, final String projectKey) {
    return

    //
    new Transformerge<FeatureType>(
        inputData.getFeatureTypesWithData(projectKey),
        new Function<FeatureType, Pipe>() {

          @Override
          public Pipe apply(FeatureType featureType) {
            return processFeatureType(inputData, projectKey, featureType);
          }

        });
  }

  private static Pipe processFeatureType(final InputData inputData, final String projectKey,
      final FeatureType featureType) {
    log.info("Processing '{}'", featureType);

    return

    // Insert feature type
    new Insert(

        // Fields to insert
        keyValuePair(TYPE_FIELD, getValue(featureType)),

        //
        new ReadableHashJoin(
            JoinData.builder()

                .joiner(new InnerJoin())

                .leftPipe(processPrimaryFiles(inputData, projectKey, featureType))
                .leftJoinFields(META_PK_FIELDS)

                .rightPipe(

                    // Meta files
                    normalizeSequencingStrategies(
                        featureType.hasSequencingStrategy(),

                        // Use feature type as replacement for a sequencing strategy if need be
                        featureType.getTypeName(),

                        processFiles(
                            inputData, projectKey, featureType.getMetaFileType(),
                            appendIfApplicable(
                                META_PK_FIELDS,
                                hasSequencingStrategy(featureType),
                                SEQUENCING_STRATEGY_FIELD))))
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
      final boolean hasSequencingStrategy, final String replacement, final Pipe pipe) {
    return hasSequencingStrategy ?
        pipe :

        // Insert a "fake" sequencing strategy to make it look uniform
        new Insert(
            keyValuePair(SEQUENCING_STRATEGY_FIELD, replacement),
            pipe
        );
  }

  private static Pipe processPrimaryFiles(
      final InputData inputData, final String projectKey, final FeatureType featureType) {
    return

    // TODO: move that before the merge to maximum parallelization (optimization) - also, use AggregateBy
    new CountBy(CountByData.builder()

        .pipe(

            //
            new GroupBy(GroupByData.builder()

                .pipe(
                    processFiles(
                        inputData, projectKey, featureType.getPrimaryFileType(),
                        META_PK_FIELDS))

                .groupByFields(META_PK_FIELDS)

                .build()))

        .countByFields(META_PK_FIELDS)
        .resultField(_ANALYSIS_OBSERVATION_COUNT_FIELD)

        .build());

  }

  private static Pipe processFiles(InputData inputData,
      final String projectKey, final FileType fileType, final Fields retainedFields) {
    return

    new Transformerge<String>(
        inputData.getMatchingFilePaths(projectKey, fileType),

        new Function<String, Pipe>() {

          int fileNumber = 0;

          @Override
          public Pipe apply(String matchingFilePath) {
            return processFile(projectKey, fileType, fileNumber++, matchingFilePath, retainedFields);
          }

        });
  }

  private static Pipe processFile(
      final String projectKey, final FileType fileType, final int fileNumber, final String matchingFilePath,
      final Fields retainedFields) {
    return new Retain(

        //
        getHeadPipe(projectKey, fileType, fileNumber),

        //
        retainedFields);
  }

  private static Pipe getHeadPipe(final String projectKey, final FileType fileType, final int fileNumber) {
    return new Pipe(getHeadPipeName(projectKey, fileType, fileNumber));
  }

  /**
   * TODO: explain!
   */
  private static String getValue(final DataType dataType) {
    return dataType.getTypeName();
  }

}
