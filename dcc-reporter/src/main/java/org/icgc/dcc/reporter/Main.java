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
    FileSystem fileSystem = getLocalFileSystem();
    val defaultParentDataDir = "/home/tony/Desktop/reports";
    val projectsJsonFilePath = "/home/tony/Desktop/reports/projects.json";
    val dictionaryFilePath = "/home/tony/tmp/dcc/0.8c/Dictionary.json";
    val inputData = InputData.from(SubmissionInputData.getMatchingFiles(
        fileSystem,
        defaultParentDataDir,
        projectsJsonFilePath,
        Dictionaries.getPatterns(
            Jackson.getJsonRoot(dictionaryFilePath))));
    System.out.println(inputData);
    // new Reporter().report("my_release", InputData.getDummy());
  }

  @SneakyThrows
  private static FileSystem getLocalFileSystem() {
    return FileSystem.get(new URI("file:///"), new Configuration());
  }
}
