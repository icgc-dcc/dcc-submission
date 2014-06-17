package org.icgc.dcc.reporter;

import java.net.URI;

import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.core.model.Dictionaries;
import org.icgc.dcc.core.util.Jackson;
import org.icgc.dcc.hadoop.dcc.SubmissionInputData;

public class Main {

  /**
   * args: my_release /home/tony/Desktop/reports /home/tony/Desktop/reports/projects.json
   * /home/tony/tmp/dcc/0.8c/Dictionary.json
   */
  public static void main(String[] args) {
    val releaseName = args[0];
    val defaultParentDataDir = args[1];
    val projectsJsonFilePath = args[2];
    val dictionaryFilePath = args[3];

    val inputData = InputData.from(
        SubmissionInputData.getMatchingFiles(
            getLocalFileSystem(),
            defaultParentDataDir,
            projectsJsonFilePath,
            Dictionaries.getPatterns(
                Jackson.getJsonRoot(dictionaryFilePath))));
    new Reporter().report(releaseName, inputData);
  }

  @SneakyThrows
  private static FileSystem getLocalFileSystem() {
    return FileSystem.get(new URI("file:///"), new Configuration());
  }
}
