package org.icgc.dcc.submission.reporter;

import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.icgc.dcc.core.util.Splitters.COMMA;

import java.util.Set;

import lombok.val;

import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.icgc.dcc.core.util.URLs;
import org.icgc.dcc.hadoop.util.HadoopConstants;

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
    val nameNodeUri = args[6];
    val jobTracker = args[7];

    Reporter.report(
        releaseName,
        getProjectKeys(projectKeys),
        defaultParentDataDir,
        projectsJsonFilePath,
        URLs.getUrlFromPath(dictionaryFilePath),
        URLs.getUrlFromPath(codeListsFilePath),
        ImmutableMap.of( // TODO: allow more params
            CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY, nameNodeUri,
            HadoopConstants.MR_JOBTRACKER_ADDRESS_KEY, jobTracker));
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
