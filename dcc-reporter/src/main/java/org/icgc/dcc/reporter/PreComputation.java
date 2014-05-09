package org.icgc.dcc.reporter;

import static org.icgc.dcc.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.core.util.FormatUtils._;
import static org.icgc.dcc.hadoop.cascading.Fields2.keyValuePair;
import static org.icgc.dcc.reporter.Reporter.getHeadPipeName;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Insert;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin.JoinData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Transformerge;

import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.CountBy;
import cascading.pipe.assembly.Retain;
import cascading.pipe.joiner.InnerJoin;
import cascading.tuple.Fields;

import com.google.common.base.Function;

@Slf4j
public class PreComputation extends SubAssembly {

  static String TYPE = "_type";
  static String PROJECT_ID = "_project_id";
  static String DONOR_ID = "donor_id";
  static String SPECIMEN_ID = "specimen_id";
  static String SAMPLE_ID = "analyzed_sample_id";
  static String ANALYSIS_ID = "analysis_id";
  static String ANALYSIS_OBSERVATION = "analysis_observation";
  static String SEQUENCING_STRATEGY = "sequencing_strategy";
  static String REDUNDANT_PREFFIX = "redundant_";
  static String COUNT_SUFFIX = "_count";
  static String ANALYSIS_OBSERVATION_COUNT = _("%s%s", ANALYSIS_OBSERVATION, COUNT_SUFFIX);

  static String REDUNDANT_SPECIMEN_ID = _("%s%s", REDUNDANT_PREFFIX, SPECIMEN_ID);
  static String REDUNDANT_SAMPLE_ID = _("%s%s", REDUNDANT_PREFFIX, SAMPLE_ID);
  static String REDUNDANT_ANALYSIS_ID = _("%s%s", REDUNDANT_PREFFIX, ANALYSIS_ID);

  static Fields CLINICAL_JOIN_FIELDS = new Fields(SPECIMEN_ID);

  PreComputation(InputData inputData) {
    setTails(process(inputData));
  }

  private static Pipe process(final InputData inputData) {
    return

    //
    new Transformerge<String>(
        inputData.getProjectKeys(),
        new Function<String, Pipe>() {

          @Override
          public Pipe apply(String projectKey) {
            return processProject(inputData, projectKey);
          }

        });
  }

  private static Pipe processProject(final InputData inputData, final String projectKey) {
    return

    // Insert project ID
    new Insert(

        // Field/value to be inserted
        keyValuePair(PROJECT_ID, projectKey),

        //
        new ReadableHashJoin(
            JoinData.builder()
                .joiner(new InnerJoin())

                .leftPipe(processFeatureTypes(inputData, projectKey))
                .leftJoinFields(new Fields(SAMPLE_ID))

                .rightPipe(processClinical(inputData, projectKey))
                .rightJoinFields(new Fields(SAMPLE_ID))

                .resultFields(
                    new Fields(
                        ANALYSIS_ID, SAMPLE_ID, ANALYSIS_OBSERVATION_COUNT, SEQUENCING_STRATEGY,
                        TYPE, DONOR_ID, SPECIMEN_ID, REDUNDANT_SAMPLE_ID))
                .discardFields(new Fields(REDUNDANT_SAMPLE_ID))

                .build()));
  }

  private static Pipe processClinical(final InputData inputData, String projectKey) {
    return

    //
    new ReadableHashJoin(
        JoinData.builder()
            .joiner(new InnerJoin())

            .leftPipe(processFiles(inputData, projectKey, SPECIMEN_TYPE, DONOR_ID, SPECIMEN_ID))
            .leftJoinFields(CLINICAL_JOIN_FIELDS)

            .rightPipe(processFiles(inputData, projectKey, SAMPLE_TYPE, SPECIMEN_ID, SAMPLE_ID))
            .rightJoinFields(CLINICAL_JOIN_FIELDS)

            .resultFields(new Fields(DONOR_ID, REDUNDANT_SPECIMEN_ID, SPECIMEN_ID, SAMPLE_ID))
            .discardFields(new Fields(REDUNDANT_SPECIMEN_ID))

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
        keyValuePair(TYPE, featureType),

        //
        new ReadableHashJoin(
            JoinData.builder()

                .joiner(new InnerJoin())

                .leftPipe(processPrimaryFiles(inputData, projectKey, featureType))
                .leftJoinFields(new Fields(ANALYSIS_ID, SAMPLE_ID))

                .rightPipe( // Meta files
                    processFiles(inputData, projectKey, featureType.getMetaFileType(),
                        ANALYSIS_ID, SAMPLE_ID, SEQUENCING_STRATEGY))
                .rightJoinFields(new Fields(ANALYSIS_ID, SAMPLE_ID))

                .resultFields(
                    new Fields(
                        ANALYSIS_ID, SAMPLE_ID, ANALYSIS_OBSERVATION_COUNT, REDUNDANT_ANALYSIS_ID,
                        REDUNDANT_SAMPLE_ID, SEQUENCING_STRATEGY))
                .discardFields(new Fields(REDUNDANT_ANALYSIS_ID, REDUNDANT_SAMPLE_ID))

                .build()));
  }

  private static CountBy processPrimaryFiles(
      final InputData inputData, final String projectKey, final FeatureType featureType) {
    return

    // TODO: move that before the merge to maximum parallelization (optimization) - also, use AggregateBy
    new CountBy(

        //
        new GroupBy( // TODO: create fluent sub-assembly for GroupBy as well?
            processFiles(inputData, projectKey, featureType.getPrimaryFileType(),
                ANALYSIS_ID, SAMPLE_ID),

            // Group by fields (group by)
            new Fields(ANALYSIS_ID, SAMPLE_ID)),

        // Group by fields (count by)
        new Fields(ANALYSIS_ID, SAMPLE_ID),

        // Result field (count by)
        new Fields(ANALYSIS_OBSERVATION_COUNT));
  }

  private static Pipe processFiles(InputData inputData,
      final String projectKey, final FileType fileType, final String... retainedFields) {
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
      final String... retainedFields) {
    return new Retain(

        //
        getHeadPipe(projectKey, fileType, fileNumber),

        //
        new Fields(retainedFields));
  }

  private static Pipe getHeadPipe(final String projectKey, final FileType fileType, final int fileNumber) {
    return new Pipe(getHeadPipeName(projectKey, fileType, fileNumber));
  }

}
