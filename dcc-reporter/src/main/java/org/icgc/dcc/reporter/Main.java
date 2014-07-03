package org.icgc.dcc.reporter;

import java.net.InetAddress;

import lombok.SneakyThrows;
import lombok.val;

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

    Reporter.report(
        releaseName,
        defaultParentDataDir,
        projectsJsonFilePath,
        dictionaryFilePath,
        codeListsFilePath);
  }

  @SneakyThrows
  public static boolean isLocal() {
    return "acroslt".equals(InetAddress.getLocalHost().getHostName()); // FIXME
  }

}
