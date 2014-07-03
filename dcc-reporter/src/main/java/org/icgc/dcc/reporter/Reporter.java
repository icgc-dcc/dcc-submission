package org.icgc.dcc.reporter;

import static com.google.common.collect.Maps.newLinkedHashMap;
import static org.icgc.dcc.core.model.Dictionaries.getMapping;
import static org.icgc.dcc.core.model.Dictionaries.getPatterns;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_M_TYPE;
import static org.icgc.dcc.core.util.Extensions.TSV;
import static org.icgc.dcc.core.util.Jackson.getJsonRoot;
import static org.icgc.dcc.core.util.Joiners.EXTENSION;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.hadoop.cascading.Fields2.getFieldName;
import static org.icgc.dcc.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.hadoop.cascading.Pipes;
import org.icgc.dcc.hadoop.dcc.SubmissionInputData;
import org.icgc.dcc.reporter.cascading.ReporterConnector;
import org.icgc.dcc.reporter.cascading.subassembly.PreComputation;
import org.icgc.dcc.reporter.cascading.subassembly.Table1;
import org.icgc.dcc.reporter.cascading.subassembly.Table2;

import cascading.pipe.Pipe;

@Slf4j
public class Reporter {

  public static final Class<Reporter> CLASS = Reporter.class;

  static String OUTPUT_DIR = "/tmp/reports";
  static String TIMESTAMP = new SimpleDateFormat("yyMMddHHmm").format(new Date()); // TODO

  public static void report(
      @NonNull final String releaseName,
      @NonNull final String defaultParentDataDir,
      @NonNull final String projectsJsonFilePath,
      @NonNull final String dictionaryFilePath,
      @NonNull final String codeListsFilePath) {

    val dictionaryRoot = getJsonRoot(dictionaryFilePath);
    val codeListsRoot = getJsonRoot(codeListsFilePath);

    val reporterInput = ReporterInput.from(
        SubmissionInputData.getMatchingFiles(
            ReporterConnector.getLocalFileSystem(),
            defaultParentDataDir,
            projectsJsonFilePath,
            getPatterns(dictionaryRoot)));

    process(
        releaseName,
        reporterInput,
        getMapping(
            dictionaryRoot,
            codeListsRoot,
            SSM_M_TYPE, // TODO: add check mapping is the same for all meta files (it should)
            getFieldName(SEQUENCING_STRATEGY_FIELD)));
  }

  public static void process(
      @NonNull String releaseName,
      @NonNull ReporterInput reporterInput,
      @NonNull Map<String, String> mapping) {
    log.info("Gathering reports: '{}' ('{}')", reporterInput, mapping);

    // Main processing
    Map<String, Pipe> table1s = newLinkedHashMap();
    Map<String, Pipe> table2s = newLinkedHashMap();
    for (val projectKey : reporterInput.getProjectKeys()) {
      val preComputationTable = new PreComputation(releaseName, reporterInput, projectKey);
      val table1 = new Table1(preComputationTable);
      val table2 = new Table2(
          preComputationTable,
          Table1.processDonors(preComputationTable),
          mapping.keySet());

      table1s.put(projectKey, table1);
      table2s.put(projectKey, table2);
    }

    ReporterConnector.connectCascade(
        reporterInput,
        table1s,
        table2s)
        .complete();

    for (val projectKey : reporterInput.getProjectKeys()) {
      ReporterGatherer.getTable(projectKey, mapping);
    }
    // log.info(table.getCsvRepresentation());
    // Gatherer.writeCsvFile(table);
  }

  public static String getHeadPipeName(String projectKey, FileType fileType, int fileNumber) {
    return Pipes.getName(projectKey, fileType.getTypeName(), fileNumber);
  }

  public static String getOutputFilePath(OutputType output, String projectKey) {
    return PATH.join(OUTPUT_DIR, getOutputFileName(output));
  }

  private static String getOutputFileName(OutputType output) {
    return EXTENSION.join(
        output.name().toLowerCase(),
        TIMESTAMP,
        TSV);
  }

}
