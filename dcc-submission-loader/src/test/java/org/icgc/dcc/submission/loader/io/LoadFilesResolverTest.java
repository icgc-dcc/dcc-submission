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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.loader.model.FileTypePath;
import org.icgc.dcc.submission.loader.util.Services;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class LoadFilesResolverTest {

  private static final String NEW_FILE_PROJECT = "ALL-US";
  private static final String OLD_FILE_PROJECT = "PACA-CA";
  private static final String RELEASE = "ICGC21";

  FileSystem fileSystem;
  File workingDir;

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  LoadFilesResolver fileScanner;

  @Before
  @SneakyThrows
  public void setUp() {
    this.fileSystem = FileSystem.getLocal(new Configuration());
    this.workingDir = tmp.newFolder("working");
    val submissionService = Services.createSubmissionService();

    this.fileScanner = new LoadFilesResolver(RELEASE, new Path(workingDir.getAbsolutePath()), fileSystem,
        submissionService);
  }

  @Test
  public void testResolveModifiedFiles() throws Exception {
    createSubmissionFile("donor.txt", NEW_FILE_PROJECT, Optional.empty());
    createSubmissionFile("sample.txt", OLD_FILE_PROJECT, Optional.of(new Date(1000L)));

    val projectFiles = fileScanner.resolveFilesToLoad(true, emptyList(), emptyList(), emptyList(), emptyList());
    assertThat(projectFiles).hasSize(1);

    val newProjectFiles = projectFiles.get(NEW_FILE_PROJECT);
    assertThat(newProjectFiles).hasSize(1);

    val donorFile = newProjectFiles.get(0).getPath();
    assertThat(donorFile.getName()).isEqualTo("donor.txt");
  }

  @Test
  public void testGetAllFiles() throws Exception {
    createSubmissionFile("donor.txt", NEW_FILE_PROJECT, Optional.empty());
    createSubmissionFile("sample.txt", NEW_FILE_PROJECT, Optional.empty());
    createSubmissionFile("specimen.txt", NEW_FILE_PROJECT, Optional.empty());
    createSubmissionFile("ssm_m.txt", NEW_FILE_PROJECT, Optional.empty());
    createSubmissionFile("ssm_p.txt", NEW_FILE_PROJECT, Optional.empty());
    createSubmissionFile("ssm_p.txt", OLD_FILE_PROJECT, Optional.of(new Date(1000L)));

    val projectFiles = fileScanner.resolveFilesToLoad(true, emptyList(), emptyList(), emptyList(), emptyList());
    log.debug("Files: {}", projectFiles);
    assertThat(projectFiles).hasSize(1);

    val newProjectFiles = projectFiles.get(NEW_FILE_PROJECT).stream()
        .map(filePath -> filePath.getPath().getName())
        .collect(toImmutableList());
    assertThat(newProjectFiles).containsOnly("donor.txt", "specimen.txt", "sample.txt", "ssm_m.txt", "ssm_p.txt");
  }

  @Test
  public void testProjectIncludes() throws IOException {
    createIncludeExcludeFilesLayout();
    val includeProjects = ImmutableList.of("ALL-US");
    val projects = fileScanner.resolveFilesToLoad(false, emptyList(), emptyList(), includeProjects, emptyList());
    assertAllUsProjectLayout(projects);
  }

  @Test
  public void testProjectExcludes() throws IOException {
    createIncludeExcludeFilesLayout();
    val excludeProjects = ImmutableList.of("PACA-US");
    val projects = fileScanner.resolveFilesToLoad(false, emptyList(), emptyList(), emptyList(), excludeProjects);
    assertAllUsProjectLayout(projects);
  }

  @Test
  public void testFilesIncludes() throws IOException {
    createIncludeExcludeFilesLayout();
    val includeFiles = ImmutableList.of("donor", "ssm_");
    val projects = fileScanner.resolveFilesToLoad(false, includeFiles, emptyList(), emptyList(), emptyList());

    assertThat(projects).hasSize(2);
    assertThat(projects.get("ALL-US")).hasSize(3);
    assertThat(projects.get("PACA-US")).hasSize(1);
  }

  @Test
  public void testFilesExcludes() throws IOException {
    createIncludeExcludeFilesLayout();
    val excludeFiles = ImmutableList.of("specimen", "sample", "sgv");
    val projects = fileScanner.resolveFilesToLoad(false, emptyList(), excludeFiles, emptyList(), emptyList());

    assertThat(projects).hasSize(2);
    assertThat(projects.get("ALL-US")).hasSize(3);
    assertThat(projects.get("PACA-US")).hasSize(1);
  }

  private void assertAllUsProjectLayout(Map<String, List<FileTypePath>> projects) {
    log.info("Include projects: {}", projects);
    assertThat(projects).hasSize(1);
    val allUs = projects.get("ALL-US").stream()
        .map(filePath -> filePath.getPath().getName())
        .collect(toImmutableList());
    assertThat(allUs).containsOnly("donor.txt", "specimen.txt", "sample.txt", "ssm_m.txt", "ssm_p.txt");
  }

  private void createIncludeExcludeFilesLayout() throws IOException {
    createSubmissionFile("donor.txt", "ALL-US", Optional.empty());
    createSubmissionFile("specimen.txt", "ALL-US", Optional.empty());
    createSubmissionFile("sample.txt", "ALL-US", Optional.empty());
    createSubmissionFile("ssm_m.txt", "ALL-US", Optional.empty());
    createSubmissionFile("ssm_p.txt", "ALL-US", Optional.empty());

    createSubmissionFile("donor.txt", "PACA-US", Optional.empty());
    createSubmissionFile("specimen.txt", "PACA-US", Optional.empty());
    createSubmissionFile("sample.txt", "PACA-US", Optional.empty());
    createSubmissionFile("sgv_m.txt", "PACA-US", Optional.empty());
    createSubmissionFile("sgv_p.txt", "PACA-US", Optional.empty());
  }

  private void createSubmissionFile(String fileName, String project, Optional<Date> modificationDate)
      throws IOException {
    val projectDir = new File(workingDir, project);
    projectDir.mkdir();

    val file = new File(projectDir, fileName);
    log.debug("Creating file {} ...", file);
    file.createNewFile();

    if (modificationDate.isPresent()) {
      file.setLastModified(modificationDate.get().getTime());
    }
  }

}
