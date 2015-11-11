package org.icgc.dcc.submission.reporter;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.io.Files.createTempDir;
import static org.icgc.dcc.common.cascading.Fields2.getFieldName;
import static org.icgc.dcc.common.core.model.Dictionaries.getMapping;
import static org.icgc.dcc.common.core.model.Dictionaries.getPatterns;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SSM_M_TYPE;
import static org.icgc.dcc.common.core.util.Extensions.TSV;
import static org.icgc.dcc.common.core.util.Joiners.EXTENSION;
import static org.icgc.dcc.common.core.util.Joiners.PATH;
import static org.icgc.dcc.common.hadoop.fs.FileSystems.getFileSystem;
import static org.icgc.dcc.common.json.Jackson.getRootObject;
import static org.icgc.dcc.submission.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;

import java.net.URL;
import java.util.Map;
import java.util.Set;

import org.icgc.dcc.common.cascading.Pipes;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.common.core.model.Identifiable;
import org.icgc.dcc.common.core.model.Identifiable.Identifiables;
import org.icgc.dcc.common.hadoop.dcc.SubmissionInputData;
import org.icgc.dcc.common.hadoop.fs.FileSystems;
import org.icgc.dcc.common.json.Jackson;
import org.icgc.dcc.submission.reporter.cascading.ReporterConnector;
import org.icgc.dcc.submission.reporter.cascading.subassembly.PreComputation;
import org.icgc.dcc.submission.reporter.cascading.subassembly.ProjectDataTypeEntity;
import org.icgc.dcc.submission.reporter.cascading.subassembly.ProjectSequencingStrategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Optional;

import cascading.pipe.Pipe;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

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
      @NonNull final Map<?, ?> hadoopProperties) {

    val dictionaryRoot = getRootObject(dictionaryFilePath);
    val codeListsRoot = Jackson.getRootArray(codeListsFilePath);

    val reporterInput = ReporterInput.from(
        SubmissionInputData.getMatchingFiles(
            getFileSystem(hadoopProperties),
            defaultParentDataDir,
            projectsJsonFilePath,
            getPatterns(dictionaryRoot)));

    return process(
        releaseName,
        projectKeys.isPresent() ? projectKeys.get() : reporterInput.getProjectKeys(),
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
      @NonNull final Map<?, ?> hadoopProperties) {
    log.info("Gathering reports for '{}.{}': '{}' ('{}')",
        new Object[] { releaseName, projectKeys, reporterInput, mapping });

    val tempDirPath = createTempDir().getAbsolutePath();
    val connector = new ReporterConnector(FileSystems.isLocal(hadoopProperties), tempDirPath);

    val preComputationCascade = connector.connectPreComputationCascade(
        reporterInput,
        releaseName,
        new PreComputation(
            releaseName, projectKeys, reporterInput.getMatchingFilePathCounts()),
        hadoopProperties);

    log.info("Running cascade");
    preComputationCascade.complete();

    val preComputationTable = getPreComputationTablePipe();
    val cascade = connector.connectFinalCascade(
        releaseName,
        reporterInput.getProjectKeys(),
        preComputationTable,
        new ProjectDataTypeEntity(preComputationTable, releaseName),
        new ProjectSequencingStrategy(
            preComputationTable, releaseName, mapping.keySet()),
        hadoopProperties);

    log.info("Running cascade");
    cascade.complete();

    log.info("Output dir: '{}'", tempDirPath);
    return tempDirPath;
  }

  private static Pipe getPreComputationTablePipe() {
    return new Pipe(
        Pipes.getName(
            Identifiables.fromStrings(
                PreComputation.class.getSimpleName())));
  }

  public static String getHeadPipeName(String projectKey, FileType fileType, int fileNumber) {
    return Pipes.getName(
        Identifiables.fromString(projectKey),
        fileType,
        Identifiables.fromInteger(fileNumber));
  }

  public static String getFilePath(
      String outputDirPath, Identifiable type, String releaseName) {
    return PATH.join(outputDirPath, getFileName(type, releaseName));
  }

  public static String getFileName(Identifiable type, String releaseName) {
    return EXTENSION.join(
        type.getId(),
        releaseName,
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
