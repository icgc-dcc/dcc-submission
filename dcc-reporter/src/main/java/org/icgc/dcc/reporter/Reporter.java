package org.icgc.dcc.reporter;

import static cascading.flow.FlowDef.flowDef;
import static org.icgc.dcc.core.util.Extensions.TSV;
import static org.icgc.dcc.core.util.Joiners.EXTENSION;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.hadoop.cascading.Pipes.getTailNames;
import static org.icgc.dcc.reporter.Connector.getRawInputTaps;
import static org.icgc.dcc.reporter.Connector.getRawOutputTaps;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.hadoop.cascading.Flows;
import org.icgc.dcc.hadoop.cascading.Pipes;

import cascading.flow.local.LocalFlowConnector;

@Slf4j
public class Reporter {

  static String OUTPUT_DIR = "/tmp/reports";
  static String OUTPUT_FILE = "/tmp/table1";

  public void report(String releaseName, InputData inputData) {
    log.info("Gathering reports: '{}'", inputData);

    // Main processing
    val tails = new StatsGathering(
        new PreComputation(releaseName, inputData))
        .getTails();

    // Connect flow
    new LocalFlowConnector()
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
        TSV);
  }

}
