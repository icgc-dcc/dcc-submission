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
package org.icgc.dcc.submission.loader.core;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableMap;
import static org.icgc.dcc.submission.loader.core.DependencyFactory.createLoadFilesResolver;
import static org.icgc.dcc.submission.loader.file.ReleaseFilesLoaderFactory.createReleaseFilesLoader;
import static org.icgc.dcc.submission.loader.util.Releases.getReleases;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.submission.loader.cli.ClientOptions;
import org.icgc.dcc.submission.loader.model.FileTypePath;
import org.icgc.dcc.submission.loader.model.Project;

import com.google.common.base.Stopwatch;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SubmissionLoader {

  public static void loadSubmission(@NonNull ClientOptions options) throws IOException {
    val dependencyFactory = DependencyFactory.getInstance();
    val releaseResolver = dependencyFactory.getReleaseResolver();
    val releases = getReleases(dependencyFactory.getFileSystem(), options.submissionDirectory, options.release);

    log.info("Loading submission files for releases: {}", releases);
    for (val release : releases) {
      val allReleaseFiles = getReleaseFiles(release, options);
      val validProjects = releaseResolver.getValidProjects(release);
      val validProjectFiles = filterValidProjectFiles(validProjects, allReleaseFiles);
      val loadProjects = filterProjectsToLoad(validProjects, validProjectFiles.keySet());

      if (validProjectFiles.isEmpty()) {
        log.info("Nothing to load for release '{}'. Skipping...", release);
        continue;
      }

      log.info("Loading release '{}'...", release);
      printFiles(validProjectFiles);

      @Cleanup
      val releaseFilesLoader = createReleaseFilesLoader(release, options.dbType);
      val watch = Stopwatch.createStarted();

      if (options.skipDbInit == false) {
        log.info("Preparing database...");
        releaseFilesLoader.prepareDb(loadProjects);
      }

      log.info("Loading files...");
      releaseFilesLoader.loadFiles(validProjectFiles);

      log.info("Finilizing database...");
      releaseFilesLoader.finalizeDatabase();

      log.info("Finished loading release '{}' in {} second(s).", release, watch.elapsed(SECONDS));
    }
  }

  private static List<Project> filterProjectsToLoad(List<Project> validProjects, Collection<String> validProjectFiles) {
    return validProjects.stream()
        .filter(p -> validProjectFiles.contains(p.getProjectId()))
        .collect(toImmutableList());
  }

  private static Map<String, List<FileTypePath>> filterValidProjectFiles(List<Project> validProjects,
      Map<String, List<FileTypePath>> releaseFiles) {
    val validProjectNames = validProjects.stream()
        .map(p -> p.getProjectId())
        .collect(toImmutableList());

    return releaseFiles.entrySet().stream()
        .filter(e -> validProjectNames.contains(e.getKey()))
        .collect(toImmutableMap(e -> e.getKey(), e -> e.getValue()));
  }

  private static Map<String, List<FileTypePath>> getReleaseFiles(String release, ClientOptions options) {
    val filesResolver = createLoadFilesResolver(options.submissionDirectory + "/" + release, release);

    return filesResolver.resolveFilesToLoad(options.newFilesOnly, options.includeFiles, options.excludeFiles,
        options.includeProjects, options.excludeProjects);
  }

  private static void printFiles(Map<String, List<FileTypePath>> projectFiles) {
    for (val entry : projectFiles.entrySet()) {
      val project = entry.getKey();
      printProjectFiles(project, entry.getValue());
    }
  }

  private static void printProjectFiles(String project, List<FileTypePath> files) {
    log.info("{}:", project);
    for (val file : files) {
      log.info("\t{}", file.getPath());
    }
  }

}
