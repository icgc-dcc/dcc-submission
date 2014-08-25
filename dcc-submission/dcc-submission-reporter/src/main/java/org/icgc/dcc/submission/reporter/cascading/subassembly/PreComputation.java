package org.icgc.dcc.submission.reporter.cascading.subassembly;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.ARGS;
import static cascading.tuple.Fields.NONE;
import static cascading.tuple.Fields.REPLACE;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.icgc.dcc.core.model.FeatureTypes.hasSequencingStrategy;
import static org.icgc.dcc.core.model.FileTypes.FileType.EXP_ARRAY_M_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.EXP_ARRAY_P_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_M_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_P_TYPE;
import static org.icgc.dcc.core.util.Strings2.NOT_APPLICABLE;
import static org.icgc.dcc.hadoop.cascading.Fields2.appendIfApplicable;
import static org.icgc.dcc.hadoop.cascading.Flows.connectFlowDef;
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

import java.io.File;
import java.net.InetAddress;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.core.model.DataType.DataTypes;
import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.core.util.Functions2;
import org.icgc.dcc.core.util.Joiners;
import org.icgc.dcc.core.util.SerializableMaps;
import org.icgc.dcc.hadoop.cascading.Flows;
import org.icgc.dcc.hadoop.cascading.connector.CascadingConnectors;
import org.icgc.dcc.hadoop.cascading.operation.BaseFunction;
import org.icgc.dcc.hadoop.cascading.taps.CascadingTaps;
import org.icgc.dcc.hadoop.util.HadoopConstants;

import cascading.flow.Flow;
import cascading.flow.FlowDef;
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

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

@Slf4j
public class PreComputation extends SubAssembly {

  public static void main(String[] args) {
    Bug.main(args);
  }

  public static class Bug {

    private static final boolean LOCAL = isLocal();

    @SneakyThrows
    private static boolean isLocal() {
      return "acroslt".equals(InetAddress.getLocalHost().getHostName());
    }

    private static final String TEST_RELEASE_NAME = "test17";
    private static final String PROJECT_KEY = "ALL-US";
    private static final Set<FileType> ALL_US_FILE_TYPES = // Don't include DONOR
        ImmutableSet.of(
            SPECIMEN_TYPE,
            SAMPLE_TYPE,
            SSM_M_TYPE,
            SSM_P_TYPE,
            EXP_ARRAY_M_TYPE,
            EXP_ARRAY_P_TYPE);

    public static void main(String[] args) {
      val preComputation = getPipe();
      val flowDef = Flows.getFlowDef(PreComputation.class);
      addSources(flowDef);
      val outputDirFilePath = addSinkTail(
          flowDef,
          preComputation.getTails()[0]);
      connect(flowDef).complete();
      log.info("done: " + outputDirFilePath);
      print(outputDirFilePath);
    }

    private static PreComputation getPipe() {
      return new PreComputation(
          TEST_RELEASE_NAME,
          PROJECT_KEY,
          SerializableMaps.asMap(
              ALL_US_FILE_TYPES,
              Functions2.<FileType, Integer> constant(1)));
    }

    private static void addSources(final FlowDef flowDef) {
      for (val fileType : ALL_US_FILE_TYPES) {
        val filePath = "/tmp/" + PROJECT_KEY + "/" + fileType.getHarmonizedOutputFileName();
        val inputTap = getTaps().getNoCompressionTsvWithHeader(filePath);
        val headPipeName = getHeadPipeName(PROJECT_KEY, fileType, 0);

        flowDef.addSource(headPipeName, inputTap);
      }
    }

    private static String addSinkTail(
        final FlowDef flowDef,
        final Pipe tail) {
      val outputDirFilePath = "/tmp/precomputation-" + new Date().getTime();
      val outputTap = getTaps().getNoCompressionTsvWithHeader(outputDirFilePath);
      flowDef.addTailSink(tail, outputTap);
      return outputDirFilePath;
    }

    private static CascadingTaps getTaps() {
      return LOCAL ? CascadingTaps.LOCAL : CascadingTaps.DISTRIBUTED;
    }

    private static Flow<?> connect(final FlowDef flowDef) {
      CascadingConnectors connectors = LOCAL ? CascadingConnectors.LOCAL : CascadingConnectors.DISTRIBUTED;
      val flowConnector = connectors.getFlowConnector(LOCAL ?
          ImmutableMap.of() :
          ImmutableMap.of(
              CommonConfigurationKeys.FS_DEFAULT_NAME_KEY, "***REMOVED***",
              HadoopConstants.MR_JOBTRACKER_ADDRESS_KEY, "***REMOVED***"));
      return connectFlowDef(flowConnector, flowDef);
    }

    @SneakyThrows
    private static void print(final String outputDirFilePath) {
      val lines =
          Files.readLines(
              new File(LOCAL ?
                  outputDirFilePath :
                  "/hdfs/dcc" + outputDirFilePath + "/part-00000"),
              Charsets.UTF_8);
      System.out.println(Joiners.INDENT.join(lines.subList(0, 10)));
      System.out.println();
      System.out.println(Joiners.INDENT.join(lines.subList(lines.size() - 11, lines.size() - 1)));
    }

  }

  private Fields META_PK_FIELDS = ANALYSIS_ID_FIELD.append(SAMPLE_ID_FIELD);
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
    return

    new Each(
        new Each(
            new Each(
                plainJoin(),
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

  private Pipe plainJoin() {
    return new Discard(
        new HashJoin(
            new Rename(
                processFeatureTypes(),
                SAMPLE_ID_FIELD,
                REDUNDANT_SAMPLE_ID_FIELD),
            REDUNDANT_SAMPLE_ID_FIELD,
            processClinicalPlain(),
            SAMPLE_ID_FIELD,
            new RightJoin()
        ),
        REDUNDANT_SAMPLE_ID_FIELD);
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
  private static Pipe normalizeSequencingStrategies(
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
  private static String getDataTypeValue(@NonNull final DataType dataType) {
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
