package org.icgc.dcc.reporter;

import static cascading.flow.FlowProps.setMaxConcurrentSteps;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.transformValues;
import static org.icgc.dcc.core.util.VersionUtils.getCommitId;
import static org.icgc.dcc.reporter.Main.isLocal;
import static org.icgc.dcc.reporter.Reporter.getOutputFilePath;

import java.util.Map;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.hadoop.cascading.taps.GenericTaps;
import org.icgc.dcc.hadoop.cascading.taps.LocalTaps;
import org.icgc.dcc.hadoop.cascading.taps.Taps;
import org.icgc.dcc.hadoop.util.HadoopConstants;
import org.icgc.dcc.hadoop.util.HadoopProperties;

import cascading.flow.FlowConnector;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.flow.local.LocalFlowConnector;
import cascading.property.AppProps;
import cascading.tap.Tap;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;

@Slf4j
public class Connector {

  private static final Taps TAPS = Main.isLocal() ? Taps.LOCAL : Taps.HADOOP;

  static FlowConnector getFlowConnector() {

    if (isLocal()) {
      log.info("Using local mode");
      return new LocalFlowConnector();
    }
    log.info("Using hadoop mode");

    Map<Object, Object> flowProperties = newHashMap();

    // From external application configuration file
    // for (val configEntry : hadoopConfig.entrySet()) {
    // flowProperties.put(configEntry.getKey(), configEntry.getValue().unwrapped());
    // }

    HadoopProperties.setHadoopUserNameProperty();

    // M/R job entry point
    AppProps.setApplicationJarClass(flowProperties, Reporter.CLASS);
    AppProps.setApplicationName(flowProperties, "TODO");
    AppProps.setApplicationVersion(flowProperties, getCommitId());

    // flowProperties =
    // enableJobOutputCompression(
    // enableIntermediateMapOutputCompression(
    // setAvailableCodecs(flowProperties),
    // SNAPPY_CODEC_PROPERTY_VALUE),
    // GZIP_CODEC_PROPERTY_VALUE);

    // CascadeDef cascadeDef = createCascadeDef("loader").setMaxConcurrentFlows(maxConcurrentFlows);
    setMaxConcurrentSteps(flowProperties, 25);

    flowProperties = HadoopProperties.enableIntermediateMapOutputCompression(
        HadoopProperties.setAvailableCodecs(flowProperties),
        HadoopConstants.LZO_CODEC_PROPERTY_VALUE);

    // flowProperties.putAll(properties);

    flowProperties.put("fs.defaultFS", "***REMOVED***");
    flowProperties.put("mapred.job.tracker", "***REMOVED***");
    flowProperties.put("mapred.child.java.opts", "-Xmx6g");

    // flowProperties.put("mapred.reduce.tasks", "20");
    // flowProperties.put("mapred.task.timeout", "1800000");
    flowProperties.put("io.sort.mb", "2000");
    flowProperties.put("io.sort.factor", "20");
    // flowProperties.put("mapred.output.compress", "true");
    // flowProperties.put("mapred.output.compression.type", "BLOCK");
    // flowProperties.put("mapred.map.output.compression.codec", "org.apache.hadoop.io.compress.SnappyCodec");
    // flowProperties.put("mapred.reduce.tasks.speculative.execution", "false");

    return new HadoopFlowConnector(flowProperties);
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  static Map<String, Tap> getRawInputTaps(InputData inputData) {
    return

    // Convert to raw taps
    transformValues(

        // Convert to pipe to tap map
        getInputTaps(

        // get pipe to path map for the project/file type combination
        inputData.getPipeNameToFilePath()),

        GenericTaps.RAW_CASTER);
  }

  /**
   * See {@link LocalTaps#RAW_CASTER}.
   */
  @SuppressWarnings("rawtypes")
  static Map<String, Tap> getRawOutputTaps(Iterable<String> tailNames) {
    return

    // Convert to raw taps
    transformValues(

        // Convert to pipe to tap map
        getOutputTaps(tailNames),

        GenericTaps.RAW_CASTER);
  }

  private static Map<String, Tap<?, ?, ?>> getInputTaps(Map<String, String> pipeNameToFilePath) {
    return transformValues(
        pipeNameToFilePath,
        new Function<String, Tap<?, ?, ?>>() {

          @Override
          public Tap<?, ?, ?> apply(final String path) {
            return TAPS.getDecompressingTsvWithHeader(path);
          }

        });
  }

  private static Map<String, Tap<?, ?, ?>> getOutputTaps(Iterable<String> tailNames) {
    val rawOutputTaps = new ImmutableMap.Builder<String, Tap<?, ?, ?>>();
    val iterator = newArrayList(tailNames).iterator();
    for (val outputType : OutputType.values()) {
      val outputFilePath = getOutputFilePath(outputType);
      rawOutputTaps.put(
          iterator.next(), // TODO: explain...
          TAPS.getNoCompressionTsvWithHeader(outputFilePath));
    }

    return rawOutputTaps.build();
  }
}
