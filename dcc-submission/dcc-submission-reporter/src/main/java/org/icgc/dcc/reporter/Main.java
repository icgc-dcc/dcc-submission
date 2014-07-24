package org.icgc.dcc.reporter;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;
import static org.icgc.dcc.core.util.Splitters.COMMA;

import java.util.Set;

import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.icgc.dcc.core.util.Protocol;
import org.icgc.dcc.hadoop.util.HadoopConstants;

import lombok.val;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

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
        getProjectKeys(projectKeys),
        defaultParentDataDir,
        projectsJsonFilePath,
        dictionaryFilePath,
        codeListsFilePath,
        ImmutableMap.of(
            FS_DEFAULT_NAME_KEY, Protocol.HDFS.getId(),
            "", "")); // TODO: read from a config file (if we still support stand-alone)
  }

  private static final Optional<Set<String>> getProjectKeys(String projectKeys) {
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

}
