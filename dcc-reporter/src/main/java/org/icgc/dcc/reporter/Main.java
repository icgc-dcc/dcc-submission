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

  public static void main(String[] args) {
    val releaseName = "my_release";
    val defaultParentDataDir = "/home/tony/Desktop/reports";
    val projectsJsonFilePath = "/home/tony/Desktop/reports/projects.json";
    val dictionaryFilePath = "/home/tony/tmp/dcc/0.8c/Dictionary.json";
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
