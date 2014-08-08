package org.icgc.dcc.reporter.cascading;

import static cascading.cascade.CascadeDef.cascadeDef;
import static cascading.flow.FlowDef.flowDef;
import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.transformValues;
import static org.icgc.dcc.reporter.Reporter.getOutputFilePath;

import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.hadoop.cascading.Cascades;
import org.icgc.dcc.hadoop.cascading.Flows;
import org.icgc.dcc.hadoop.cascading.connector.CascadingConnectors;
import org.icgc.dcc.hadoop.cascading.taps.GenericTaps;
import org.icgc.dcc.hadoop.cascading.taps.LocalTaps;
import org.icgc.dcc.hadoop.cascading.taps.CascadingTaps;
import org.icgc.dcc.hadoop.util.HadoopConstants;
import org.icgc.dcc.hadoop.util.HadoopProperties;
import org.icgc.dcc.reporter.OutputType;
import org.icgc.dcc.reporter.Reporter;
import org.icgc.dcc.reporter.ReporterInput;

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
    
    val cascadeDef = cascadeDef()
        .setName(Cascades.getName(Reporter.CLASS))
        .setMaxConcurrentFlows(maxConcurrentFlows);

    for (val projectKey : reporterInput.getProjectKeys()) {
      val projectDataTypeEntity = projectDataTypeEntities.get(projectKey);
      val projectSequencingStrategy = projectSequencingStrategies.get(projectKey);
      cascadeDef.addFlow(
          getFlowConnector().connect(flowDef()
            .addSources(getRawInputTaps(reporterInput, projectKey))
            .addTailSink(
                projectDataTypeEntity,
                getRawOutputProjectDataTypeEntityTap(projectDataTypeEntity.getName(), releaseName, projectKey))
            .addTailSink(
                projectSequencingStrategy,
                getRawOutputProjectSequencingStrategyTap(projectSequencingStrategy.getName(), releaseName, projectKey))
            .setName(Flows.getName(Reporter.CLASS, projectKey))));
    }

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

  private FlowConnector getFlowConnector() {    
    Map<Object, Object> flowProperties = newHashMap();
    HadoopProperties.setHadoopUserNameProperty();
    flowProperties = getClusterFlowProperties(flowProperties);
    
    return connector.getFlowConnector(flowProperties);
  }

  private static Map<Object, Object> getClusterFlowProperties(@NonNull final Map<Object, Object> flowProperties) {
    return ImmutableMap.builder()
        
        .putAll(flowProperties)
        .putAll(
            HadoopProperties.enableIntermediateMapOutputCompression(
              HadoopProperties.setAvailableCodecs(flowProperties),
              HadoopConstants.LZO_CODEC_PROPERTY_VALUE))
    
        .build();
    
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

}
