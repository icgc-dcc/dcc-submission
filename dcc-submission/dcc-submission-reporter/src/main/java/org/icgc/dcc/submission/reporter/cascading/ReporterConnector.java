package org.icgc.dcc.submission.reporter.cascading;

import static cascading.cascade.CascadeDef.cascadeDef;
import static cascading.flow.FlowDef.flowDef;
import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Maps.transformValues;
import static org.icgc.dcc.core.util.Jackson.formatPrettyJson;
import static org.icgc.dcc.submission.reporter.IntermediateOutputType.PRE_COMPUTATION;
import static org.icgc.dcc.submission.reporter.IntermediateOutputType.PRE_COMPUTATION_CLINICAL;
import static org.icgc.dcc.submission.reporter.IntermediateOutputType.PRE_COMPUTATION_FEATURE_TYPES;
import static org.icgc.dcc.submission.reporter.IntermediateOutputType.PRE_COMPUTATION_TMP1;
import static org.icgc.dcc.submission.reporter.IntermediateOutputType.PRE_COMPUTATION_TMP2;
import static org.icgc.dcc.submission.reporter.IntermediateOutputType.PRE_PROCESSING_ALL;
import static org.icgc.dcc.submission.reporter.IntermediateOutputType.PRE_PROCESSING_FEATURE_TYPES;
import static org.icgc.dcc.submission.reporter.Reporter.getOutputFilePath;

import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.hadoop.cascading.Cascades;
import org.icgc.dcc.hadoop.cascading.Flows;
import org.icgc.dcc.hadoop.cascading.connector.CascadingConnectors;
import org.icgc.dcc.hadoop.cascading.taps.CascadingTaps;
import org.icgc.dcc.hadoop.cascading.taps.GenericTaps;
import org.icgc.dcc.hadoop.cascading.taps.LocalTaps;
import org.icgc.dcc.hadoop.util.HadoopConstants;
import org.icgc.dcc.hadoop.util.HadoopProperties;
import org.icgc.dcc.submission.reporter.IntermediateOutputType;
import org.icgc.dcc.submission.reporter.OutputType;
import org.icgc.dcc.submission.reporter.Reporter;
import org.icgc.dcc.submission.reporter.ReporterInput;
import org.icgc.dcc.submission.reporter.cascading.subassembly.projectdatatypeentity.Dumps;

import cascading.cascade.Cascade;
import cascading.flow.FlowConnector;
import cascading.pipe.Pipe;
import cascading.tap.Tap;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

@Slf4j
public class ReporterConnector {

  private static final String CONCURRENCY = String.valueOf(5);

  private final CascadingTaps taps;
  private final CascadingConnectors connector;

  private final String outputDirPath;

  public ReporterConnector(
      final boolean local,
      @NonNull final String outputDirPath) {
    this.taps = local ? CascadingTaps.LOCAL : CascadingTaps.DISTRIBUTED;
    this.connector = local ? CascadingConnectors.LOCAL : CascadingConnectors.DISTRIBUTED;
    this.outputDirPath = outputDirPath;
    log.info(connector.describe());
  }

  public Cascade connectCascade(
      @NonNull final ReporterInput reporterInput,
      @NonNull final String releaseName,
      @NonNull final Map<String, Pipe> projectDataTypeEntities,
      @NonNull final Map<String, Pipe> projectSequencingStrategies,
      @NonNull final Map<String, String> hadoopProperties) {

    val maxConcurrentFlows = getConcurrency();
    log.info("maxConcurrentFlows: '{}'", maxConcurrentFlows);
    log.info("hadoopProperties: '{}'", hadoopProperties);
    for (val projectKey : reporterInput.getProjectKeys()) {
      log.info(formatPrettyJson(reporterInput.getPipeNameToFilePath(projectKey)));

      // TODO: same for outputs
    }

    val cascadeDef = cascadeDef()
        .setName(Cascades.getName(Reporter.CLASS))
        .setMaxConcurrentFlows(maxConcurrentFlows);

    for (val projectKey : reporterInput.getProjectKeys()) {
      val projectDataTypeEntity = projectDataTypeEntities.get(projectKey);
      val projectSequencingStrategy = projectSequencingStrategies.get(projectKey);
      cascadeDef.addFlow(
          getFlowConnector(hadoopProperties).connect(
              flowDef()
                  .addSources(getRawInputTaps(reporterInput, projectKey))
                  .addTailSink(
                      Dumps.HACK_TABLE.get(PRE_COMPUTATION_CLINICAL, projectKey),
                      getRawIntermediateOutputTap(releaseName, projectKey, PRE_COMPUTATION_CLINICAL))
                  .addTailSink(
                      Dumps.HACK_TABLE.get(PRE_COMPUTATION_FEATURE_TYPES, projectKey),
                      getRawIntermediateOutputTap(releaseName, projectKey, PRE_COMPUTATION_FEATURE_TYPES))
                  .addTailSink(
                      Dumps.HACK_TABLE.get(PRE_COMPUTATION, projectKey),
                      getRawIntermediateOutputTap(releaseName, projectKey, PRE_COMPUTATION))
                  .addTailSink(
                      Dumps.HACK_TABLE.get(PRE_COMPUTATION_TMP1, projectKey),
                      getRawIntermediateOutputTap(releaseName, projectKey, PRE_COMPUTATION_TMP1))
                  .addTailSink(
                      Dumps.HACK_TABLE.get(PRE_COMPUTATION_TMP2, projectKey),
                      getRawIntermediateOutputTap(releaseName, projectKey, PRE_COMPUTATION_TMP2))
                  .addTailSink(
                      Dumps.HACK_TABLE.get(PRE_PROCESSING_ALL, projectKey),
                      getRawIntermediateOutputTap(releaseName, projectKey, PRE_PROCESSING_ALL))
                  .addTailSink(
                      Dumps.HACK_TABLE.get(PRE_PROCESSING_FEATURE_TYPES, projectKey),
                      getRawIntermediateOutputTap(releaseName, projectKey, PRE_PROCESSING_FEATURE_TYPES))
                  .addTailSink(
                      projectDataTypeEntity,
                      getRawOutputProjectDataTypeEntityTap(projectDataTypeEntity.getName(), releaseName, projectKey))
                  .addTailSink(
                      projectSequencingStrategy,
                      getRawOutputProjectSequencingStrategyTap(projectSequencingStrategy.getName(), releaseName,
                          projectKey))
                  .setName(Flows.getName(Reporter.CLASS, projectKey))));
    }

    HadoopProperties.setHadoopUserNameProperty();

    log.info("Connecting cascade");
    return connector
        .getCascadeConnector(hadoopProperties)
        .connect(cascadeDef);
  }

  private Integer getConcurrency() {
    return Integer.valueOf(firstNonNull(
        // To ease benchmarking until we find the sweet spot
        System.getProperty("DCC_REPORT_CONCURRENCY"),
        CONCURRENCY));
  }

  private FlowConnector getFlowConnector(@NonNull final Map<String, String> hadoopProperties) {
    return connector.getFlowConnector(ImmutableMap.builder()

        .putAll(hadoopProperties)
        .putAll(
            HadoopProperties.enableIntermediateMapOutputCompression(
                HadoopProperties.setAvailableCodecs(newLinkedHashMap()),
                HadoopConstants.LZO_CODEC_PROPERTY_VALUE))

        .build());
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private Map<String, Tap> getRawInputTaps(ReporterInput reporterInput, String projectKey) {
    return

    // Convert to raw taps
    transformValues(

        // Convert to pipe to tap map
        getInputTaps(

        // get pipe to path map for the project/file type combination
        reporterInput.getPipeNameToFilePath(projectKey)),

        GenericTaps.RAW_CASTER);
  }

  private Map<String, Tap<?, ?, ?>> getInputTaps(
      @NonNull final Map<String, String> pipeNameToFilePath) {

    return transformValues(
        pipeNameToFilePath,
        new Function<String, Tap<?, ?, ?>>() {

          @Override
          public Tap<?, ?, ?> apply(final String path) {
            return taps.getDecompressingTsvWithHeader(path);
          }

        });
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private Tap getRawOutputProjectDataTypeEntityTap(
      @NonNull final String tailName,
      @NonNull final String releaseName,
      @NonNull final String projectKey) {
    return GenericTaps.RAW_CASTER.apply(getOutputProjectDataTypeEntityTap(tailName, releaseName, projectKey));
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private Tap getRawIntermediateOutputTap(
      @NonNull final String releaseName,
      @NonNull final String projectKey,
      @NonNull final IntermediateOutputType intermediateOutputType) {
    return GenericTaps.RAW_CASTER.apply(getIntermediateOutputTap(intermediateOutputType, releaseName, projectKey));
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private Tap getRawOutputProjectSequencingStrategyTap(
      @NonNull final String tailName,
      @NonNull final String releaseName,
      @NonNull final String projectKey) {
    return GenericTaps.RAW_CASTER.apply(getOutputProjectSequencingStrategyTap(tailName, releaseName, projectKey));
  }

  private Tap<?, ?, ?> getOutputProjectDataTypeEntityTap(
      @NonNull final String tailName,
      @NonNull final String releaseName,
      @NonNull final String projectKey) {
    val outputFilePath = getOutputFilePath(outputDirPath, OutputType.DONOR, releaseName, projectKey);
    return taps.getNoCompressionTsvWithHeader(outputFilePath);
  }

  private Tap<?, ?, ?> getOutputProjectSequencingStrategyTap(
      @NonNull final String tailName,
      @NonNull final String releaseName,
      @NonNull final String projectKey) {
    val outputFilePath = getOutputFilePath(outputDirPath, OutputType.SEQUENCING_STRATEGY, releaseName, projectKey);
    return taps.getNoCompressionTsvWithHeader(outputFilePath);
  }

  private Tap<?, ?, ?> getIntermediateOutputTap(IntermediateOutputType intermediateOutputType, String releaseName,
      String projectKey) {
    val outputFilePath = getOutputFilePath(outputDirPath, intermediateOutputType, releaseName, projectKey);
    return taps.getNoCompressionTsvWithHeader(outputFilePath);
  }

}
