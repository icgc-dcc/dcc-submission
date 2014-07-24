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
import org.icgc.dcc.hadoop.cascading.connector.CascadingConnector;
import org.icgc.dcc.hadoop.cascading.taps.GenericTaps;
import org.icgc.dcc.hadoop.cascading.taps.LocalTaps;
import org.icgc.dcc.hadoop.cascading.taps.Taps;
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
  
  private final Taps taps; 
  private final CascadingConnector connector;   
  private final String outputDirPath;
  
  public ReporterConnector(
      final boolean local,
      @NonNull final String outputDirPath) {
    this.taps = local ? Taps.LOCAL : Taps.HADOOP;
    this.connector = local ? CascadingConnector.LOCAL : CascadingConnector.CLUSTER;
    this.outputDirPath = outputDirPath;
    log.info(connector.describe());
  }
  
  public Cascade connectCascade(
      @NonNull final ReporterInput reporterInput,
      @NonNull final Map<String, Pipe> table1s,
      @NonNull final Map<String, Pipe> table2s,
      @NonNull final Map<String, String> hadoopProperties) {
    
    val maxConcurrentFlows = getConcurrency();
    log.info("maxConcurrentFlows: '{}'", maxConcurrentFlows);
    
    val cascadeDef = cascadeDef()
        .setName(Cascades.getName(Reporter.CLASS))
        .setMaxConcurrentFlows(maxConcurrentFlows);

    for (val projectKey : reporterInput.getProjectKeys()) {
      val table1 = table1s.get(projectKey);
      val table2 = table2s.get(projectKey);
      cascadeDef.addFlow(
          getFlowConnector().connect(flowDef()
            .addSources(getRawInputTaps(reporterInput, projectKey))
            .addTailSink(
                table1,
                getRawOutputTable1Tap(table1.getName(), projectKey))
            .addTailSink(
                table2,
                getRawOutputTable2Tap(table2.getName(), projectKey))
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
        
        .put(HadoopConstants.IO_SORT_MB, "2000")
        .put(HadoopConstants.IO_SORT_FACTOR, "20")
    
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
  private Tap getRawOutputTable1Tap(String tailName, String projectKey) {
    return GenericTaps.RAW_CASTER.apply(getOutputTable1Tap(tailName, projectKey));
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private Tap getRawOutputTable2Tap(String tailName, String projectKey) {
    return GenericTaps.RAW_CASTER.apply(getOutputTable2Tap(tailName, projectKey));
  }

  private Tap<?, ?, ?> getOutputTable1Tap(String tailName, String projectKey) {
    val outputFilePath = getOutputFilePath(outputDirPath, OutputType.DONOR, projectKey);
    return taps.getNoCompressionTsvWithHeader(outputFilePath);
  }

  private Tap<?, ?, ?> getOutputTable2Tap(String tailName, String projectKey) {
    val outputFilePath = getOutputFilePath(outputDirPath, OutputType.SEQUENCING_STRATEGY, projectKey);
    return taps.getNoCompressionTsvWithHeader(outputFilePath);
  }

}
