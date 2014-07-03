package org.icgc.dcc.reporter;

import static org.icgc.dcc.core.model.Dictionaries.getMapping;
import static org.icgc.dcc.core.model.Dictionaries.getPatterns;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_M_TYPE;
import static org.icgc.dcc.core.util.Jackson.getJsonRoot;
import static org.icgc.dcc.hadoop.cascading.Fields2.getFieldName;
import static org.icgc.dcc.reporter.Reporter.report;
import static org.icgc.dcc.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;

import java.net.InetAddress;
import java.net.URI;

import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.hadoop.dcc.SubmissionInputData;

public class Main {

  /**
   * args: my_release /home/tony/Desktop/reports /home/tony/Desktop/reports/projects.json
   * /home/tony/tmp/dcc/0.8c/Dictionary.json /home/tony/tmp/dcc/0.8c/CodeList.json
   */
  public static void main(String[] args) {
    val releaseName = args[0];
    val defaultParentDataDir = args[1];
    val projectsJsonFilePath = args[2];
    val dictionaryFilePath = args[3];
    val codeListsFilePath = args[4];

    val dictionaryRoot = getJsonRoot(dictionaryFilePath);
    val codeListsRoot = getJsonRoot(codeListsFilePath);

    val reporterInputData = ReporterInputData.from(
        SubmissionInputData.getMatchingFiles(
            getLocalFileSystem(),
            defaultParentDataDir,
            projectsJsonFilePath,
            getPatterns(dictionaryRoot)));

    report(
        releaseName,
        reporterInputData,
        getMapping(
            dictionaryRoot,
            codeListsRoot,
            SSM_M_TYPE, // TODO: add check mapping is the same for all meta files (it should)
            getFieldName(SEQUENCING_STRATEGY_FIELD)));
  }

  @SneakyThrows
  private static FileSystem getLocalFileSystem() {
    return FileSystem.get(new URI("file:///"), new Configuration());
  }

  @SneakyThrows
  public static boolean isLocal() {
    return "acroslt".equals(InetAddress.getLocalHost().getHostName()); // FIXME
  }

}
