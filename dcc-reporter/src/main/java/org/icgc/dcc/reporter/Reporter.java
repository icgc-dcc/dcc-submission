package org.icgc.dcc.reporter;

import static org.icgc.dcc.core.util.Extensions.TSV;
import static org.icgc.dcc.core.util.Joiners.EXTENSION;
import static org.icgc.dcc.core.util.Joiners.PATH;

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
import org.icgc.dcc.hadoop.cascading.Pipes;
import org.icgc.dcc.reporter.cascading.ReporterConnector;
import org.icgc.dcc.reporter.cascading.subassembly.PreComputation;
import org.icgc.dcc.reporter.cascading.subassembly.Table1;
import org.icgc.dcc.reporter.cascading.subassembly.Table2;

import cascading.flow.hadoop.util.HadoopUtil;

@Slf4j
public class Reporter {

  public static final Class<Reporter> CLASS = Reporter.class;

  static String OUTPUT_DIR = "/tmp/reports";
  static String TIMESTAMP = new SimpleDateFormat("yyMMddHHmm").format(new Date()); // TODO

  public static void report(
      @NonNull String releaseName,
      @NonNull ReporterInputData reporterInputData,
      @NonNull Map<String, String> mapping) {
    log.info("Gathering reports: '{}' ('{}')", reporterInputData, mapping);

    // Main processing
    val preComputationTable = new PreComputation(releaseName, reporterInputData);
    val table1 = new Table1(preComputationTable);
    val table2 = new Table2(
        preComputationTable,
        Table1.processDonors(preComputationTable),
        mapping.keySet());

    ReporterConnector.connectFlow(
        reporterInputData,
        table1,
        table2)
        .complete();

    ReporterGatherer.getTable(reporterInputData.getProjectKeys(), mapping);
    // log.info(table.getCsvRepresentation());
    // Gatherer.writeCsvFile(table);
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
    cascadingSerialize(new PreComputation("bla", ReporterInputData.getDummy()));
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
