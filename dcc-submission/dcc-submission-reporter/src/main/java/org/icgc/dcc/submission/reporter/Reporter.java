package org.icgc.dcc.submission.reporter;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.io.Files.createTempDir;
import static org.icgc.dcc.core.model.Dictionaries.getMapping;
import static org.icgc.dcc.core.model.Dictionaries.getPatterns;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_M_TYPE;
import static org.icgc.dcc.core.util.Extensions.TSV;
import static org.icgc.dcc.core.util.Jackson.getRootObject;
import static org.icgc.dcc.core.util.Joiners.EXTENSION;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.hadoop.cascading.Fields2.getFieldName;
import static org.icgc.dcc.submission.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.TYPE_FIELD;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.core.util.Jackson;
import org.icgc.dcc.hadoop.cascading.Pipes;
import org.icgc.dcc.hadoop.dcc.SubmissionInputData;
import org.icgc.dcc.hadoop.fs.FileSystems;
import org.icgc.dcc.submission.reporter.cascading.ReporterConnector;
import org.icgc.dcc.submission.reporter.cascading.subassembly.PreComputation;
import org.icgc.dcc.submission.reporter.cascading.subassembly.ClinicalCounts;
import org.icgc.dcc.submission.reporter.cascading.subassembly.ProjectDataTypeEntity;
import org.icgc.dcc.submission.reporter.cascading.subassembly.ProjectSequencingStrategy;

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
      @NonNull final URL dictionaryFilePath,
      @NonNull final URL codeListsFilePath,
      @NonNull final Map<String, String> hadoopProperties) {

    val dictionaryRoot = getRootObject(dictionaryFilePath);
    val codeListsRoot = Jackson.getRootArray(codeListsFilePath);

    val reporterInput = ReporterInput.from(
        SubmissionInputData.getMatchingFiles(
            FileSystems.getFileSystem("file://localhost"),
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
        new Object[] { releaseName, projectKeys, reporterInput, mapping });

    // Main processing
    val projectDataTypeEntities = Maps.<String, Pipe> newLinkedHashMap();
    val projectSequencingStrategies = Maps.<String, Pipe> newLinkedHashMap();
    for (val projectKey : projectKeys) {
      val preComputationTable = new PreComputation(releaseName, projectKey, reporterInput);
      val projectDataTypeEntity = new ProjectDataTypeEntity(preComputationTable);
      val projectSequencingStrategy = new ProjectSequencingStrategy(
          preComputationTable,
          ClinicalCounts.donor(
              preComputationTable,
              PROJECT_ID_FIELD.append(TYPE_FIELD)),
          mapping.keySet());

      projectDataTypeEntities.put(projectKey, projectDataTypeEntity);
      projectSequencingStrategies.put(projectKey, projectSequencingStrategy);
    }

    System.out.println("===========================================================================");
    for (val projectKey : projectKeys) {
      Map<String, String> pipeNameToFilePath = reporterInput.getPipeNameToFilePath(projectKey);
      System.out.println(projectKey);
      System.out.println(Jackson.formatPrettyJson(pipeNameToFilePath));
    }
    System.out.println("===========================================================================");

    val outputDir = createTempDir();
    val connectCascade = new ReporterConnector(
        FileSystems.isLocal(hadoopProperties),
        outputDir.getAbsolutePath())
        .connectCascade(
            reporterInput,
            releaseName,
            projectDataTypeEntities,
            projectSequencingStrategies,
            hadoopProperties);

    log.info("Running cascade");
    connectCascade.complete();

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
