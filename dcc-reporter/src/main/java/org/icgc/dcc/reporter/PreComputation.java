package org.icgc.dcc.reporter;

import static org.icgc.dcc.core.model.FieldNames.ReporterFieldNames.RELEASE_NAME;
import static org.icgc.dcc.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.core.util.Strings2.UNIX_NEW_LINE;
import static org.icgc.dcc.hadoop.cascading.Fields2.getCountFieldCounterpart;
import static org.icgc.dcc.hadoop.cascading.Fields2.getRedundantFieldCounterpart;
import static org.icgc.dcc.hadoop.cascading.Fields2.keyValuePair;
import static org.icgc.dcc.reporter.Reporter.getHeadPipeName;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.CountBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.CountBy.CountByData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.GroupBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.GroupBy.GroupByData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Insert;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin.JoinData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Transformerge;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.TupleEntriesLogger;

import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Retain;
import cascading.pipe.joiner.InnerJoin;
import cascading.pipe.joiner.RightJoin;
import cascading.tuple.Fields;

import com.google.common.base.Function;
import com.google.common.base.Optional;

@Slf4j
public class PreComputation extends SubAssembly {

  static Fields TYPE_FIELD = new Fields("_type");
  static Fields PROJECT_ID_FIELD = new Fields("_project_id");
  static Fields DONOR_ID_FIELD = new Fields("donor_id");
  static Fields SPECIMEN_ID_FIELD = new Fields("specimen_id");
  static Fields SAMPLE_ID_FIELD = new Fields("analyzed_sample_id");
  static Fields ANALYSIS_ID_FIELD = new Fields("analysis_id");
  static Fields ANALYSIS_OBSERVATION_FIELD = new Fields("analysis_observation");

  static Fields SEQUENCING_STRATEGY_FIELD = new Fields("sequencing_strategy");
  static Fields ANALYSIS_OBSERVATION_COUNT_FIELD = getCountFieldCounterpart(ANALYSIS_OBSERVATION_FIELD);

  static Fields REDUNDANT_SPECIMEN_ID_FIELD = getRedundantFieldCounterpart(SPECIMEN_ID_FIELD);
  static Fields REDUNDANT_SAMPLE_ID_FIELD = getRedundantFieldCounterpart(SAMPLE_ID_FIELD);
  static Fields REDUNDANT_ANALYSIS_ID_FIELD = getRedundantFieldCounterpart(ANALYSIS_ID_FIELD);

  static Fields META_PK_FIELDS =
      ANALYSIS_ID_FIELD
          .append(SAMPLE_ID_FIELD);

  PreComputation(String releaseName, InputData inputData) {
    setTails(new TupleEntriesLogger(
        Optional.of(UNIX_NEW_LINE),
        process(releaseName, inputData))); // TODO: add if?
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
                        .append(ANALYSIS_OBSERVATION_COUNT_FIELD)
                        .append(SEQUENCING_STRATEGY_FIELD)
                        .append(TYPE_FIELD)
                        .append(DONOR_ID_FIELD)
                        .append(SPECIMEN_ID_FIELD)
                        .append(REDUNDANT_SAMPLE_ID_FIELD))
                .discardFields(REDUNDANT_SAMPLE_ID_FIELD)

                .build()));
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
        keyValuePair(TYPE_FIELD, featureType),

        //
        new ReadableHashJoin(
            JoinData.builder()

                .joiner(new InnerJoin())

                .leftPipe(processPrimaryFiles(inputData, projectKey, featureType))
                .leftJoinFields(META_PK_FIELDS)

                .rightPipe( // Meta files
                    processFiles(
                        inputData, projectKey, featureType.getMetaFileType(),
                        META_PK_FIELDS
                            .append(SEQUENCING_STRATEGY_FIELD)))
                .rightJoinFields(META_PK_FIELDS)

                .resultFields(
                    META_PK_FIELDS
                        .append(ANALYSIS_OBSERVATION_COUNT_FIELD)
                        .append(REDUNDANT_ANALYSIS_ID_FIELD)
                        .append(REDUNDANT_SAMPLE_ID_FIELD)
                        .append(SEQUENCING_STRATEGY_FIELD))
                .discardFields(
                    REDUNDANT_ANALYSIS_ID_FIELD
                        .append(REDUNDANT_SAMPLE_ID_FIELD))

                .build()));
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
        .resultField(ANALYSIS_OBSERVATION_COUNT_FIELD)

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

}
