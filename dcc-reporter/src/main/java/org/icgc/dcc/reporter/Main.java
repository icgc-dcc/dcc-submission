package org.icgc.dcc.reporter;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.icgc.dcc.core.util.Splitters.COMMA;

import java.net.InetAddress;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.val;

import com.google.common.base.Optional;

public class Main {

  private static final String ALL_PROJECTS_SHORTHAND1 = "all";
  private static final String ALL_PROJECTS_SHORTHAND2 = "-";

  public static void main(String[] args) {
    val releaseName = args[0];
    val projectKeys = args[1];
    val defaultParentDataDir = args[2];
    val projectsJsonFilePath = args[3];
    val dictionaryFilePath = args[4];
    val codeListsFilePath = args[5];

    Reporter.report(
        releaseName,
        foo(projectKeys),
        defaultParentDataDir,
        projectsJsonFilePath,
        dictionaryFilePath,
        codeListsFilePath);
  }

  private static final Optional<Set<String>> foo(String projectKeys) {
    return isAllProjects(projectKeys) ?
        Optional.<Set<String>> absent() :
        Optional.<Set<String>> of(newLinkedHashSet(COMMA.split(projectKeys)));
  }

  private static boolean isAllProjects(String projectKeys) {
    return projectKeys == null
        || projectKeys.isEmpty()
        || ALL_PROJECTS_SHORTHAND1.equalsIgnoreCase(projectKeys)
        || ALL_PROJECTS_SHORTHAND2.equals(projectKeys);
  }

  @SneakyThrows
  public static boolean isLocal() {
    return "acroslt".equals(InetAddress.getLocalHost().getHostName()); // FIXME
  }

}
