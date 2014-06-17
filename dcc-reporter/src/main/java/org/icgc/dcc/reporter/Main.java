package org.icgc.dcc.reporter;

import lombok.val;

import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.core.model.Dictionaries;
import org.icgc.dcc.core.util.Jackson;
import org.icgc.dcc.hadoop.dcc.SubmissionInputData;

public class Main {

  public static void main(String[] args) {

    FileSystem fileSystem = null;
    val defaultParentDataDir = "";
    val projectsJsonFilePath = "";
    val dictionaryFilePath = "/home/tony/tmp/dcc/0.8c/Dictionary.json";

    // new Reporter().report("my_release", InputData.getDummy());

    val p = SubmissionInputData.getMatchingFiles(
        fileSystem,
        defaultParentDataDir,
        projectsJsonFilePath,
        Dictionaries.getPatterns(
            Jackson.getJsonRoot(dictionaryFilePath)));
    System.out.println(p);
  }

}
