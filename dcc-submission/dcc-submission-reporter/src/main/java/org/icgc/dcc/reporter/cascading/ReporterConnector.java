package org.icgc.dcc.reporter.cascading;

import static cascading.cascade.CascadeDef.cascadeDef;
import static cascading.flow.FlowDef.flowDef;
import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.transformValues;
import static org.icgc.dcc.core.util.VersionUtils.getCommitId;
import static org.icgc.dcc.reporter.Main.isLocal;
import static org.icgc.dcc.reporter.Reporter.getOutputFilePath;

import java.net.URI;
import java.util.Map;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.hadoop.cascading.connector.CascadingConnector;
import org.icgc.dcc.hadoop.cascading.taps.GenericTaps;
import org.icgc.dcc.hadoop.cascading.taps.LocalTaps;
import org.icgc.dcc.hadoop.cascading.taps.Taps;
import org.icgc.dcc.hadoop.util.HadoopConstants;
import org.icgc.dcc.hadoop.util.HadoopProperties;
import org.icgc.dcc.reporter.Main;
import org.icgc.dcc.reporter.OutputType;
import org.icgc.dcc.reporter.Reporter;
import org.icgc.dcc.reporter.ReporterInput;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.cascade.CascadeDef;
import cascading.flow.FlowConnector;
import cascading.flow.FlowDef;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.flow.local.LocalFlowConnector;
import cascading.pipe.Pipe;
import cascading.property.AppProps;
import cascading.tap.Tap;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

@Slf4j
public class ReporterConnector {

  private static final String CONCURRENCY = String.valueOf(5);
  private static final Taps TAPS = Main.isLocal() ? Taps.LOCAL : Taps.HADOOP;
  private static final CascadingConnector CONNECTOR = Main.isLocal() ? CascadingConnector.LOCAL : CascadingConnector.CLUSTER;
  private static final String NAMENODE = Main.isLocal() ? "file://localhost" : "***REMOVED***";
  private static final String JOB_TRACKER = Main.isLocal() ? "localhost" : "***REMOVED***";   

  public static Cascade connectCascade(
      @NonNull final ReporterInput reporterInput,
      @NonNull final Map<String, Pipe> table1s,
      @NonNull final Map<String, Pipe> table2s) {

    val maxConcurrentFlows = Integer.valueOf(firstNonNull(
        // To ease benchmarking until we find the sweet spot
        System.getProperty("DCC_REPORT_CONCURRENCY"),
        CONCURRENCY));
    log.info("maxConcurrentFlows: '{}'", maxConcurrentFlows);
    CascadeDef cascadeDef = cascadeDef().setName("reportscascade")
        .setMaxConcurrentFlows(maxConcurrentFlows);

    for (val projectKey : reporterInput.getProjectKeys()) {
      val table1 = table1s.get(projectKey);
      val table2 = table2s.get(projectKey);
      FlowDef flowDef = flowDef()
          .addSources(getRawInputTaps(reporterInput, projectKey))
          .addTailSink(table1, getRawOutputTable1Tap(table1.getName(), projectKey))
          .addTailSink(table2, getRawOutputTable2Tap(table2.getName(), projectKey))
          .setName("flow-" + projectKey);
      cascadeDef.addFlow(getFlowConnector().connect(flowDef));
    }

    return new CascadeConnector().connect(cascadeDef);
  }

  
  /**
   * TODO: refactoring with the other components.
   */
  private static FlowConnector getFlowConnector() {
    log.info(CONNECTOR.describe());
    
    Map<Object, Object> flowProperties = newHashMap();
    if (!Main.isLocal()) {
      HadoopProperties.setHadoopUserNameProperty();
      flowProperties = getClusterFlowProperties(flowProperties);
    }
    
    return CONNECTOR.getFlowConnector(flowProperties);
  }

  private static Map<Object, Object> getClusterFlowProperties(Map<Object, Object> flowProperties) {
    AppProps.setApplicationJarClass(flowProperties, Reporter.CLASS);
    AppProps.setApplicationName(flowProperties, Reporter.CLASS.getSimpleName());
    AppProps.setApplicationVersion(flowProperties, getCommitId());
    flowProperties = HadoopProperties.enableIntermediateMapOutputCompression(
        HadoopProperties.setAvailableCodecs(flowProperties),
        HadoopConstants.LZO_CODEC_PROPERTY_VALUE);
    flowProperties.put("fs.defaultFS", NAMENODE);
    flowProperties.put("mapred.job.tracker", JOB_TRACKER);
    flowProperties.put("mapred.child.java.opts", "-Xmx6g");
    flowProperties.put("io.sort.mb", "2000");
    flowProperties.put("io.sort.factor", "20");
    return flowProperties;
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private static Map<String, Tap> getRawInputTaps(ReporterInput reporterInput, String projectKey) {
    return

    // Convert to raw taps
    transformValues(

        // Convert to pipe to tap map
        getInputTaps(

        // get pipe to path map for the project/file type combination
        reporterInput.getPipeNameToFilePath(projectKey)),

        GenericTaps.RAW_CASTER);
  }

  private static Map<String, Tap<?, ?, ?>> getInputTaps(
      @NonNull final Map<String, String> pipeNameToFilePath) {

    return transformValues(
        pipeNameToFilePath,
        new Function<String, Tap<?, ?, ?>>() {

          @Override
          public Tap<?, ?, ?> apply(final String path) {
            return TAPS.getDecompressingTsvWithHeader(path);
          }

        });
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private static Tap getRawOutputTable1Tap(String tailName, String projectKey) {
    return GenericTaps.RAW_CASTER.apply(getOutputTable1Tap(tailName, projectKey));
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private static Tap getRawOutputTable2Tap(String tailName, String projectKey) {
    return GenericTaps.RAW_CASTER.apply(getOutputTable2Tap(tailName, projectKey));
  }

  private static Tap<?, ?, ?> getOutputTable1Tap(String tailName, String projectKey) {
    val outputFilePath = getOutputFilePath(OutputType.DONOR, projectKey);
    return TAPS.getNoCompressionTsvWithHeader(outputFilePath);
  }

  private static Tap<?, ?, ?> getOutputTable2Tap(String tailName, String projectKey) {
    val outputFilePath = getOutputFilePath(OutputType.SEQUENCING_STRATEGY, projectKey);
    return TAPS.getNoCompressionTsvWithHeader(outputFilePath);
  }

}
