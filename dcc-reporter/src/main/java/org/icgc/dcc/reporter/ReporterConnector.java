package org.icgc.dcc.reporter;

import static cascading.flow.FlowDef.flowDef;
import static cascading.flow.FlowProps.setMaxConcurrentSteps;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.transformValues;
import static org.icgc.dcc.core.util.VersionUtils.getCommitId;
import static org.icgc.dcc.reporter.Main.isLocal;
import static org.icgc.dcc.reporter.Reporter.getOutputFilePath;

import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.hadoop.cascading.taps.GenericTaps;
import org.icgc.dcc.hadoop.cascading.taps.LocalTaps;
import org.icgc.dcc.hadoop.cascading.taps.Taps;
import org.icgc.dcc.hadoop.util.HadoopConstants;
import org.icgc.dcc.hadoop.util.HadoopProperties;
import org.icgc.dcc.reporter.cascading.subassembly.Table1;
import org.icgc.dcc.reporter.cascading.subassembly.Table2;

import cascading.flow.Flow;
import cascading.flow.FlowConnector;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.flow.local.LocalFlowConnector;
import cascading.property.AppProps;
import cascading.tap.Tap;

import com.google.common.base.Function;

@Slf4j
public class ReporterConnector {

  private static final int NUM_CONCURRENT_STEPS = 25;
  private static final Taps TAPS = Main.isLocal() ? Taps.LOCAL : Taps.HADOOP;

  public static Flow<?> connectFlow(
      @NonNull final InputData inputData,
      @NonNull final Table1 table1,
      @NonNull final Table2 table2,
      @NonNull final String flowName) {

    return getFlowConnector()
        .connect(
            flowDef()
                .addSources(getRawInputTaps(inputData))
                .addTailSink(table1, ReporterConnector.getRawOutputTap(table1.getName()))
                .addTailSink(table2, ReporterConnector.getRawOutputTap2(table2.getName()))
                .setName(flowName));
  }

  private static FlowConnector getFlowConnector() {

    if (isLocal()) {
      log.info("Using local mode");
      return new LocalFlowConnector();
    }
    log.info("Using hadoop mode");

    Map<Object, Object> flowProperties = newHashMap();
    HadoopProperties.setHadoopUserNameProperty();

    AppProps.setApplicationJarClass(flowProperties, Reporter.CLASS);
    AppProps.setApplicationName(flowProperties, Reporter.CLASS.getSimpleName());
    AppProps.setApplicationVersion(flowProperties, getCommitId());
    setMaxConcurrentSteps(flowProperties, NUM_CONCURRENT_STEPS);
    flowProperties = HadoopProperties.enableIntermediateMapOutputCompression(
        HadoopProperties.setAvailableCodecs(flowProperties),
        HadoopConstants.LZO_CODEC_PROPERTY_VALUE);
    flowProperties.put("fs.defaultFS", "***REMOVED***");
    flowProperties.put("mapred.job.tracker", "***REMOVED***");
    flowProperties.put("mapred.child.java.opts", "-Xmx6g");
    flowProperties.put("io.sort.mb", "2000");
    flowProperties.put("io.sort.factor", "20");

    return new HadoopFlowConnector(flowProperties);
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private static Map<String, Tap> getRawInputTaps(InputData inputData) {
    return

    // Convert to raw taps
    transformValues(

        // Convert to pipe to tap map
        getInputTaps(

        // get pipe to path map for the project/file type combination
        inputData.getPipeNameToFilePath()),

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
  private static Tap getRawOutputTap(String tailName) {
    return GenericTaps.RAW_CASTER.apply(getOutputTap(tailName));
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  private static Tap getRawOutputTap2(String tailName) {
    return GenericTaps.RAW_CASTER.apply(getOutputTap2(tailName));
  }

  private static Tap<?, ?, ?> getOutputTap(String tailName) {
    val outputFilePath = getOutputFilePath(OutputType.DONOR);
    return TAPS.getNoCompressionTsvWithHeader(outputFilePath);
  }

  private static Tap<?, ?, ?> getOutputTap2(String tailName) {
    val outputFilePath = getOutputFilePath(OutputType.SEQUENCING_STRATEGY);
    return TAPS.getNoCompressionTsvWithHeader(outputFilePath);
  }

}
