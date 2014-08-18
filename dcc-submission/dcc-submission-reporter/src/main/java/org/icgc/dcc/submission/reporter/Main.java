package org.icgc.dcc.submission.reporter;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.icgc.dcc.core.model.Configurations.HADOOP_KEY;
import static org.icgc.dcc.core.util.Splitters.COMMA;

import java.io.File;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.util.Jackson;
import org.icgc.dcc.core.util.URLs;

import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    val configFilePath = args[6];

    Reporter.report(
        releaseName,
        getProjectKeys(projectKeys),
        defaultParentDataDir,
        projectsJsonFilePath,
        URLs.getUrl(dictionaryFilePath),
        URLs.getUrl(codeListsFilePath),
        ImmutableMap.of(
            "fs.defaultFS", "hdfs://localhost:8020", //
            "mapred.job.tracker", "localhost:8021"
            ));
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

  private static Map<String, String> getHadoopProperties(@NonNull final String configFilePath) {
    val config = Jackson.readFile(new File(configFilePath));
    checkState(config.getNodeType() == JsonNodeType.OBJECT);
    return Jackson.asMap((ObjectNode) config.path(HADOOP_KEY));
  }

}
