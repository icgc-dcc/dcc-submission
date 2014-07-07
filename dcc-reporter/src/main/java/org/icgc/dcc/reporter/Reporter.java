package org.icgc.dcc.reporter;

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
import java.util.Set;

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

import com.google.common.base.Optional;
import com.google.common.collect.Maps;

@Slf4j
public class Reporter {

  public static final Class<Reporter> CLASS = Reporter.class;

  static String OUTPUT_DIR = "/tmp/reports";
  static String TIMESTAMP = new SimpleDateFormat("yyMMddHHmm").format(new Date()); // TODO

  public static void report(
      @NonNull final String releaseName,
      @NonNull final Optional<Set<String>> projectKeys,
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
        projectKeys.isPresent() ?
            projectKeys.get() :
            reporterInput.getProjectKeys(),
        reporterInput,
        getMapping(
            dictionaryRoot,
            codeListsRoot,
            SSM_M_TYPE, // TODO: add check mapping is the same for all meta files (it should)
            getFieldName(SEQUENCING_STRATEGY_FIELD)));
  }

  public static void process(
      @NonNull String releaseName,
      @NonNull Set<String> projectKeys,
      @NonNull ReporterInput reporterInput,
      @NonNull Map<String, String> mapping) {
    log.info("Gathering reports: '{}' ('{}')", reporterInput, mapping);

    // Main processing
    val table1s = Maps.<String, Pipe> newLinkedHashMap();
    val table2s = Maps.<String, Pipe> newLinkedHashMap();
    for (val projectKey : projectKeys) {
      val preComputationTable = new PreComputation(releaseName, projectKey, reporterInput);
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

    for (val projectKey : projectKeys) {
      ReporterGatherer.getJsonTable1(projectKey);
    }
    for (val projectKey : projectKeys) {
      ReporterGatherer.getJsonTable2(projectKey, mapping);
    }
    System.out.println(ReporterGatherer.getTsvTable1(projectKeys));
    System.out.println(ReporterGatherer.getTsvTable2(projectKeys, mapping));
    // log.info(table.getCsvRepresentation());
    // Gatherer.writeCsvFile(table);
  }

  public static String getHeadPipeName(String projectKey, FileType fileType, int fileNumber) {
    return Pipes.getName(projectKey, fileType.getTypeName(), fileNumber);
  }

  private static final String FUSE_MOUTPOINT_PREFIX = "/hdfs/dcc";
  private static final String PART_FILE = "part-00000";

  public static String getOuputFileFusePath(OutputType output, String projectKey) {
    String outputFilePath = Reporter.getOutputFilePath(output, projectKey);
    if (!Main.isLocal()) {
      outputFilePath = PATH.join(
          FUSE_MOUTPOINT_PREFIX,
          outputFilePath,
          PART_FILE);
    }
    return outputFilePath;
  }

  public static String getOutputFilePath(OutputType output, String projectKey) {
    return PATH.join(OUTPUT_DIR, getOutputFileName(output, projectKey));
  }

  private static String getOutputFileName(OutputType output, String projectKey) {
    return EXTENSION.join(
        output.name().toLowerCase(),
        projectKey,
        TIMESTAMP,
        TSV);
  }

}
