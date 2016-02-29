/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.loader.io;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.lsDir;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.lsFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.loader.meta.SubmissionMetadataService;
import org.icgc.dcc.submission.loader.model.FileTypePath;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@Slf4j
@RequiredArgsConstructor
public class LoadFilesResolver {

  /**
   * Constants.
   */
  private static final String PROJECT_NAME_REGEX = "^\\w{2,4}-\\w{2}$";
  private static final Instant START_DATE = getStartDate();

  /**
   * Configuration.
   */
  @NonNull
  private final String release;
  @NonNull
  private final Path inputDir;

  /**
   * Dependencies.
   */
  @NonNull
  private final FileSystem fs;
  @NonNull
  private final SubmissionMetadataService submissionMetadataService;

  public Map<String, List<FileTypePath>> resolveFilesToLoad(boolean recentOnly, @NonNull List<String> includeFiles,
      @NonNull List<String> excludeFiles, @NonNull List<String> includeProjects, @NonNull List<String> excludeProjects) {
    val projectFiles = ImmutableMap.<String, List<FileTypePath>> builder();
    val projectPaths = lsDir(fs, inputDir);
    val projects = projectPaths.stream()
        .map(f -> f.getName())
        .collect(toImmutableList());
    log.debug("Resolved projects: {}", projects);

    for (val projectPath : projectPaths) {
      val projectName = projectPath.getName();
      log.debug("Resolving '{}' project files...", projectName);
      if (!isValidProjectName(projectName) || isSkipEntity(projectName, includeProjects, excludeProjects)) {
        log.debug("Skipping invalid project '{}'...", projectName);
        continue;
      }

      val filesToLoad = resolveFiles(recentOnly, projectPath, includeFiles, excludeFiles);
      if (!filesToLoad.isEmpty()) {
        projectFiles.put(projectName, filesToLoad);
      }
    }

    return projectFiles.build();
  }

  @SneakyThrows
  private List<FileTypePath> resolveFiles(boolean recentOnly, Path projectDir, List<String> includeFiles,
      List<String> excludeFiles) {
    val filesToLoad = ImmutableList.<FileTypePath> builder();
    val allFiles = lsFile(fs, projectDir);
    log.debug("'{}' project files: {}", projectDir.getName(), allFiles);

    val filePatterns = submissionMetadataService.getFilePatterns();
    for (val file : allFiles) {
      val fileName = file.getName();
      val fileType = resolveFileType(fileName, filePatterns);
      if (fileType.isPresent() == false) {
        log.debug("Skipping invalid file {}...", file);
        continue;
      }

      if ((recentOnly && !isNewFile(file))
          || isSkipEntity(fileName, includeFiles, excludeFiles)) {
        log.debug("Skipping old file {}...", file);
        continue;
      }

      log.debug("Adding file {} ...", file);
      filesToLoad.add(new FileTypePath(fileType.get(), file));
    }

    return filesToLoad.build();
  }

  private static boolean isSkipEntity(String entityName, List<String> includeRegexes, List<String> excludeRegexes) {
    // Exclude matches
    if (excludeRegexes.isEmpty() == false) {
      for (val excludeRegex : excludeRegexes) {
        if (entityName.contains(excludeRegex)) {
          log.debug("Excluding '{}' because of the exclude rules.", entityName);
          return true;
        }
      }
    }

    if (includeRegexes.isEmpty() == false) {
      for (val includeRegex : includeRegexes) {
        if (entityName.contains(includeRegex)) {
          log.debug("Including '{}' because of the include rules.", entityName);
          // Include matches
          return false;
        }
      }

      log.debug("Excluding '{}' because it's not defined in the include rules.", entityName);
      // Include is defined but did not match.
      return true;
    }

    // Default case
    return false;
  }

  @SneakyThrows
  private boolean isNewFile(Path file) {
    val fileStatus = fs.getFileStatus(file);

    return isNewFile(fileStatus.getModificationTime());
  }

  private static Optional<String> resolveFileType(String name, Map<String, String> filePatterns) {
    for (val entry : filePatterns.entrySet()) {
      val filePattern = entry.getValue();
      if (name.matches(filePattern)) {
        return Optional.of(entry.getKey());
      }
    }

    return Optional.empty();
  }

  private static boolean isValidProjectName(String projectName) {
    return projectName.matches(PROJECT_NAME_REGEX);
  }

  private static boolean isNewFile(long modificationTimeMilli) {
    val fileTime = Instant.ofEpochMilli(modificationTimeMilli);
    log.debug("File time: {}. Start date: {}", fileTime, START_DATE);

    return fileTime.isAfter(START_DATE);
  }

  private static Instant getStartDate() {
    val now = Instant.now();
    val startDate = now.minus(1L, DAYS);
    log.debug("New files start date: {}", startDate);

    return startDate;
  }

}
