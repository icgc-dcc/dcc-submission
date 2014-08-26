package org.icgc.dcc.hadoop.dcc;

/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.transformValues;
import static java.util.Collections.sort;
import static java.util.regex.Pattern.compile;
import static org.icgc.dcc.core.util.FormatUtils._;
import static org.icgc.dcc.core.util.Jackson.formatPrettyJson;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.lsAll;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.FileTypes.FileType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Table;

/**
 * Parses a JSON file describing where the submission data can be found, in combination with a default parent directory
 * (that may or may not be overwritten).
 * <p>
 * TODO: find a better module to put this in?
 */
@Slf4j
@RequiredArgsConstructor
public class SubmissionInputData {

  private static final String PARENT_DIR_PARAMETER = "parent_dir";

  /**
   * TODO: use {@link Table} rather?
   */
  public static Map<String, Map<FileType, List<Path>>> getMatchingFiles(
      FileSystem fileSystem,
      String defaultParentDataDir,
      String projectsJsonFilePath,
      Map<FileType, String> patterns) {
    val projectDescriptions = getProjectDescriptions(projectsJsonFilePath);
    val projectKeys = getProjectKeys(projectDescriptions);
    checkSanity(projectDescriptions, projectKeys);
    log.info("Processing projects: '{}'", projectKeys);
    log.info("Patterns: '{}'", patterns);
    log.info("Default data dir: '{}'", defaultParentDataDir);

    val projectToFiles = getMatchingFiles(
        projectKeys, projectDescriptions, patterns, fileSystem, defaultParentDataDir);
    log.info("projectToFiles: '{}'", getDisplayString(projectToFiles));
    return projectToFiles;
  }

  /**
   * This is called form submission-server where there is no access to to the projects json file.
   * 
   * FIXME: should really factor out the projectDescriptions stuff in the main getMatchingFiles
   */
  public static Map<String, Map<FileType, List<Path>>> getMatchingFiles(
      FileSystem fileSystem,
      String defaultParentDataDir,
      Set<String> projectKeys,
      Map<FileType, String> patterns) {

    val projectsJson = JsonNodeFactory.instance.objectNode();
    for (val projectKey : projectKeys) {
      projectsJson.with(projectKey);
    }
    return getMatchingFiles(projectKeys, projectsJson, patterns, fileSystem, defaultParentDataDir);
  }

  /**
   * Returns the mapping of matching files on a per project/per file type basis.
   */
  private static Map<String, Map<FileType, List<Path>>> getMatchingFiles(
      Set<String> projectKeys, ObjectNode projectDescriptions, Map<FileType, String> patterns,
      FileSystem fileSystem, String defaultParentDataDir) {

    val matchingFiles = new LinkedHashMap<String, Map<FileType, List<Path>>>();
    for (val projectKey : projectKeys) {
      log.info("Finding matching files for project: '{}'", projectKey);

      val projectDescription = projectDescriptions.get(projectKey);
      val projectDataDirPath = getProjectDataDir(
          projectDescription.has(PARENT_DIR_PARAMETER) ?
              projectDescription.get(PARENT_DIR_PARAMETER).asText() : defaultParentDataDir,
          projectKey);
      log.info("Using data dir: '{}'", projectDataDirPath);

      val fileTypeToFiles = new LinkedHashMap<FileType, List<Path>>();
      matchingFiles.put(projectKey, fileTypeToFiles);

      for (val entry : patterns.entrySet()) {
        val fileType = entry.getKey();
        val pattern = entry.getValue();
        fileTypeToFiles.put(fileType, listMatchingFiles(fileSystem, projectDataDirPath, pattern));
      }

      for (val fileType : FileType.values()) {
        val keyName = getKeyName(fileType);
        if (projectDescription.has(keyName)) {
          val filePath = projectDescription.get(keyName).asText();
          log.info("Using '{}' overwrite: '{}'", fileType, filePath);
          fileTypeToFiles.put(fileType, newArrayList(new Path(filePath)));
        } else {
          log.info("No '{}' overwrite", fileType);
        }
      }
    }
    return matchingFiles;
  }

  private static List<Path> listMatchingFiles(FileSystem fileSystem, String dirPath, String pattern) {
    val projectDataFiles = lsAll(fileSystem, new Path(dirPath), compile(pattern));
    sort(projectDataFiles, new Comparator<Path>() {

      @Override
      public int compare(Path path1, Path path2) {
        return path1.toUri().getPath().compareTo(path2.toUri().getPath());
      }

    });
    return projectDataFiles;
  }

  @SneakyThrows
  private static ObjectNode getProjectDescriptions(String projectsJsonFilePath) {
    return (ObjectNode) new ObjectMapper().readTree(new File(projectsJsonFilePath));
  }

  private static Set<String> getProjectKeys(@NonNull final ObjectNode projectDescriptions) {
    val projectKeys = new ImmutableSet.Builder<String>();
    val entries = projectDescriptions.fields();
    while (entries.hasNext()) {
      val entry = entries.next();
      val projectKey = entry.getKey();
      projectKeys.add(projectKey);
    }

    return projectKeys.build();
  }

  private static void checkSanity(ObjectNode projectDescriptions, Set<String> projectKeys) {
    for (val projectKey : projectKeys) {
      val projectDescription = projectDescriptions.get(projectKey);
      val parameters = newArrayList(projectDescription.fieldNames());
      parameters.remove(PARENT_DIR_PARAMETER);
      for (val fileType : FileType.values()) {
        parameters.remove(getKeyName(fileType));
      }
      checkState(parameters.isEmpty(), "Unknown parameters specified: ", parameters);
    }
  }

  private static String getProjectDataDir(@NonNull String parentDir, @NonNull String projectKey) {
    checkArgument(!parentDir.isEmpty());
    return PATH.join(parentDir, projectKey);
  }

  private static String getKeyName(FileType fileType) {
    return _("%s_file", fileType.getId());
  }

  @SneakyThrows
  private static String getDisplayString(Map<String, Map<FileType, List<Path>>> matchingFiles) {
    return formatPrettyJson(transformMap(matchingFiles));
  }

  private static Map<String, Map<FileType, List<String>>> transformMap(
      Map<String, Map<FileType, List<Path>>> matchingFiles) {
    return transformValues(matchingFiles,
        new Function<Map<FileType, List<Path>>, Map<FileType, List<String>>>() {

          @Override
          public Map<FileType, List<String>> apply(Map<FileType, List<Path>> typeToPaths) {
            return transformValues(typeToPaths, new Function<List<Path>, List<String>>() {

              @Override
              public List<String> apply(List<Path> paths) {
                return newArrayList(transform(paths, new Function<Path, String>() {

                  @Override
                  public String apply(Path path) {
                    return path.toUri().getPath();
                  }

                }));
              }

            });
          }

        });
  }
}
