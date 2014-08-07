package org.icgc.dcc.reporter;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.io.Files.createTempDir;
import static org.icgc.dcc.core.model.Dictionaries.getMapping;
import static org.icgc.dcc.core.model.Dictionaries.getPatterns;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_M_TYPE;
import static org.icgc.dcc.core.util.Extensions.TSV;
import static org.icgc.dcc.core.util.Jackson.getJsonRoot;
import static org.icgc.dcc.core.util.Joiners.EXTENSION;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.hadoop.cascading.Fields2.getFieldName;
import static org.icgc.dcc.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;

import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.hadoop.cascading.Pipes;
import org.icgc.dcc.hadoop.dcc.SubmissionInputData;
import org.icgc.dcc.hadoop.fs.FileSystems;
import org.icgc.dcc.reporter.cascading.ReporterConnector;
import org.icgc.dcc.reporter.cascading.subassembly.PreComputation;
import org.icgc.dcc.reporter.cascading.subassembly.ProcessClinicalType;
import org.icgc.dcc.reporter.cascading.subassembly.Table2;
import org.icgc.dcc.reporter.cascading.subassembly.table1.Table1;

import cascading.pipe.Pipe;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

@Slf4j
public class Reporter {

  public static final Class<Reporter> CLASS = Reporter.class;

  public static String report(
      @NonNull final String releaseName,
      @NonNull final Optional<Set<String>> projectKeys,
      @NonNull final String defaultParentDataDir,
      @NonNull final String projectsJsonFilePath,
      @NonNull final String dictionaryFilePath,
      @NonNull final String codeListsFilePath,
      @NonNull final Map<String, String> hadoopProperties) {

    val dictionaryRoot = getJsonRoot(dictionaryFilePath);
    val codeListsRoot = getJsonRoot(codeListsFilePath);

    val reporterInput = ReporterInput.from(
        SubmissionInputData.getMatchingFiles(
            FileSystems.getLocalFileSystem(),
            defaultParentDataDir,
            projectsJsonFilePath,
            getPatterns(dictionaryRoot)));

    return process(
        releaseName,
        projectKeys.isPresent() ?
            projectKeys.get() :
            reporterInput.getProjectKeys(),
        reporterInput,
        getSequencingStrategyMapping(
            dictionaryRoot,
            codeListsRoot),
            hadoopProperties);
  }

  public static String process(
      @NonNull final String releaseName,
      @NonNull final Set<String> projectKeys,
      @NonNull final ReporterInput reporterInput,
      @NonNull final Map<String, String> mapping,
      @NonNull final Map<String, String> hadoopProperties) {
    log.info("Gathering reports for '{}.{}': '{}' ('{}')",
        new Object[] {releaseName, projectKeys, reporterInput, mapping});
    
    // Main processing
    val table1s = Maps.<String, Pipe> newLinkedHashMap();
    val table2s = Maps.<String, Pipe> newLinkedHashMap();
    for (val projectKey : projectKeys) {
      val preComputationTable = new PreComputation(releaseName, projectKey, reporterInput);
      val table1 = new Table1(preComputationTable);
      val table2 = new Table2(
          preComputationTable,
          ProcessClinicalType.donor(preComputationTable),
          mapping.keySet());

      table1s.put(projectKey, table1);
      table2s.put(projectKey, table2);
    }

    val outputDir = createTempDir();
    new ReporterConnector(
          FileSystems.isLocal(hadoopProperties),
          outputDir.getAbsolutePath())
        .connectCascade(
            reporterInput,
            releaseName,
            table1s,
            table2s,
            hadoopProperties)
        .complete();
    
    return outputDir.getAbsolutePath();
  }

  public static String getHeadPipeName(String projectKey, FileType fileType, int fileNumber) {
    return Pipes.getName(projectKey, fileType.getTypeName(), fileNumber);
  }

  public static String getOutputFilePath(
      String outputDirPath, OutputType output, String releaseName, String projectKey) {
    return PATH.join(outputDirPath, getOutputFileName(output, releaseName, projectKey));
  }

  public static String getOutputFileName(OutputType output, String releaseName, String projectKey) {
    return EXTENSION.join(
        output.name().toLowerCase(),
        releaseName,
        projectKey,
        TSV);
  }

  private static Map<String, String> getSequencingStrategyMapping(
      @NonNull final JsonNode dictionaryRoot,
      @NonNull final JsonNode codeListsRoot) {
    val sequencingStrategyMapping = getMapping(
        dictionaryRoot,
        codeListsRoot,
        SSM_M_TYPE, // TODO: add check mapping is the same for all meta files (it should)
        getFieldName(SEQUENCING_STRATEGY_FIELD));
    checkState(sequencingStrategyMapping.isPresent(),
        "Expecting codelist to exists for: '%s.%s'",
        SSM_M_TYPE, SEQUENCING_STRATEGY_FIELD);

    return sequencingStrategyMapping.get();
  }

}
