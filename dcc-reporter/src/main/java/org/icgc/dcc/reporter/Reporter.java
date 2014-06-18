package org.icgc.dcc.reporter;

import static cascading.flow.FlowDef.flowDef;
import static com.google.common.collect.Maps.newHashMap;
import static org.icgc.dcc.core.util.Extensions.TSV;
import static org.icgc.dcc.core.util.Joiners.EXTENSION;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.hadoop.cascading.Pipes.getTailNames;
import static org.icgc.dcc.reporter.Connector.getRawInputTaps;
import static org.icgc.dcc.reporter.Connector.getRawOutputTaps;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.hadoop.cascading.Flows;
import org.icgc.dcc.hadoop.cascading.Pipes;
import org.icgc.dcc.hadoop.util.HadoopProperties;

import cascading.flow.FlowConnector;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.flow.hadoop.util.HadoopUtil;
import cascading.property.AppProps;

@Slf4j
public class Reporter {

  static String OUTPUT_DIR = "/tmp/reports";
  static String OUTPUT_FILE = "/tmp/table1";
  static String TIMESTAMP = new SimpleDateFormat("yyMMddHHmm").format(new Date()); // TODO

  public void report(@NonNull String releaseName, @NonNull InputData inputData) {
    log.info("Gathering reports: '{}'", inputData);

    // Main processing
    val tails = new StatsGathering(
        new PreComputation(releaseName, inputData))
        .getTails();

    // Connect flow
    getFlowConnector(null)
        .connect(
            flowDef()
                .addSources(getRawInputTaps(inputData))
                .addSinks(getRawOutputTaps(getTailNames(tails)))
                .addTails(tails)
                .setName(Flows.getName(getClass())))
        .complete();

    val table = Gatherer
        .getTable(inputData.getProjectKeys());
    log.info(table.getCsvRepresentation());
    Gatherer.writeCsvFile(table);
  }

  public static String getHeadPipeName(String projectKey, FileType fileType, int fileNumber) {
    return Pipes.getName(projectKey, fileType.getTypeName(), fileNumber);
  }

  public static String getOutputFilePath(OutputType output) {
    return PATH.join(OUTPUT_DIR, getOutputFileName(output));
  }

  private static String getOutputFileName(OutputType output) {
    return EXTENSION.join(
        output.name().toLowerCase(),
        TIMESTAMP,
        TSV);
  }

  public FlowConnector getFlowConnector(Map<Object, Object> properties) {
    Map<Object, Object> flowProperties = newHashMap();

    // From external application configuration file
    // for (val configEntry : hadoopConfig.entrySet()) {
    // flowProperties.put(configEntry.getKey(), configEntry.getValue().unwrapped());
    // }

    // M/R job entry point
    AppProps.setApplicationJarClass(flowProperties, this.getClass());

    // flowProperties =
    // enableJobOutputCompression(
    // enableIntermediateMapOutputCompression(
    // setAvailableCodecs(flowProperties),
    // SNAPPY_CODEC_PROPERTY_VALUE),
    // GZIP_CODEC_PROPERTY_VALUE);

    flowProperties = HadoopProperties.setAvailableCodecs(flowProperties);

    // flowProperties.putAll(properties);

    flowProperties.put("fs.defaultFS", "***REMOVED***");
    flowProperties.put("mapred.job.tracker ", "***REMOVED***");
    flowProperties.put("mapred.child.java.opts", "-Xmx6g");

    // flowProperties.put("mapred.reduce.tasks", "20");
    // flowProperties.put("mapred.task.timeout", "1800000");
    // flowProperties.put("io.sort.mb", "200");
    // flowProperties.put("io.sort.factor", "20");
    // flowProperties.put("mapred.output.compress", "true");
    // flowProperties.put("mapred.output.compression.type", "BLOCK");
    // flowProperties.put("mapred.map.output.compression.codec", "org.apache.hadoop.io.compress.SnappyCodec");
    // flowProperties.put("mapred.reduce.tasks.speculative.execution", "false");

    return new HadoopFlowConnector(flowProperties);
  }

  /**
   * Method that reproduces what Cascading does to serialize a job step.
   * 
   * @param object the object to serialize.
   * @return the base 64 encoded object
   */
  @SneakyThrows
  private static String cascadingSerialize(Object object) {
    return HadoopUtil.serializeBase64(object, new JobConf(new Configuration()));
  }

  public static void main(String[] args) {
    cascadingSerialize(new PreComputation("bla", InputData.getDummy()));
    // cascadingSerialize(new StatsGathering(preComputationTable);
  }

  // @Test
  // public void testSerializable() throws URISyntaxException {
  // val task = new DistributedDocumentTask(RELEASE_TYPE, config);
  //
  // val serialized = cascadingSerialize(task);
  // log.info("task: {}, serialized: {}", task, serialized);
  // assertThat(serialized).isNotEmpty();
  //
  // val deserialized = cascadingDeserialize(serialized);
  // log.info("task: {}, deserialized: {}", task, deserialized);
  // assertThat(deserialized).isEqualTo(task);
  // }

}
