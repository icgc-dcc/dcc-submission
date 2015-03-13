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
package org.icgc.dcc.submission.reporter;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.Component.REPORTER;
import static org.icgc.dcc.common.core.DccResources.getCodeListsDccResource;
import static org.icgc.dcc.common.core.DccResources.getDictionaryDccResource;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.CNSM_M_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.CNSM_P_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.EXP_ARRAY_M_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.EXP_ARRAY_P_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.JCN_M_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.JCN_P_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SSM_M_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SSM_P_TYPE;
import static org.icgc.dcc.common.core.util.Jackson.formatPrettyJson;
import static org.icgc.dcc.common.core.util.Joiners.DOT;
import static org.icgc.dcc.common.core.util.Joiners.EXTENSION;
import static org.icgc.dcc.common.core.util.Joiners.NEWLINE;
import static org.icgc.dcc.common.core.util.Joiners.PATH;
import static org.icgc.dcc.common.core.util.Separators.HASHTAG;
import static org.icgc.dcc.common.hadoop.fs.FileSystems.getDefaultLocalFileSystem;
import static org.icgc.dcc.common.test.Tests.CONF_DIR_NAME;
import static org.icgc.dcc.common.test.Tests.DATA_DIR_NAME;
import static org.icgc.dcc.common.test.Tests.MAVEN_TEST_RESOURCES_DIR;
import static org.icgc.dcc.common.test.Tests.PROJECT1;
import static org.icgc.dcc.common.test.Tests.PROJECT2;
import static org.icgc.dcc.common.test.Tests.PROJECTS_JSON_FILE_NAME;
import static org.icgc.dcc.common.test.Tests.TEST_PATCH_NUMBER;
import static org.icgc.dcc.common.test.Tests.getTestReleasePrefix;
import static org.icgc.dcc.submission.reporter.OutputType.DONOR;
import static org.icgc.dcc.submission.reporter.OutputType.PRE_COMPUTATION;
import static org.icgc.dcc.submission.reporter.OutputType.SEQUENCING_STRATEGY;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.Component;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.common.core.model.Identifiable;
import org.icgc.dcc.common.core.util.Collections3;
import org.icgc.dcc.common.core.util.Extensions;
import org.icgc.dcc.common.core.util.Optionals;
import org.icgc.dcc.common.hadoop.cascading.CascadingContext;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

/**
 * TODO: add checks
 */
@Slf4j
public class ReporterTest {

  private static final Component TESTED_COMPONENT = REPORTER;
  private static final String TEST_RELEASE_NAME = getTestReleasePrefix(TESTED_COMPONENT) + TEST_PATCH_NUMBER;
  private static final String DEFAULT_PARENT_TEST_DIR = PATH.join(MAVEN_TEST_RESOURCES_DIR, DATA_DIR_NAME);
  private static final String TEST_CONF_DIR = PATH.join(MAVEN_TEST_RESOURCES_DIR, CONF_DIR_NAME);
  private static final String TEST_FILE = PATH.join(DEFAULT_PARENT_TEST_DIR, "dummy");
  private static final Map<String, String> EMPTY_MAP = ImmutableMap.<String, String> of();
  private static final String FILE_SEPARATOR = "---";
  private static final List<Identifiable> FILES = ImmutableList.<Identifiable> of(
      SPECIMEN_TYPE, SAMPLE_TYPE,
      SSM_M_TYPE, CNSM_M_TYPE, JCN_M_TYPE, EXP_ARRAY_M_TYPE,
      SSM_P_TYPE, CNSM_P_TYPE, JCN_P_TYPE, EXP_ARRAY_P_TYPE,
      PRE_COMPUTATION, DONOR, SEQUENCING_STRATEGY);

  private File tempDir;

  @Before
  public void setUp() {
    tempDir = Files.createTempDir();
    val files = getData(readLines(new File(TEST_FILE)));
    new File(PATH.join(tempDir, PROJECT1)).mkdir();
    new File(PATH.join(tempDir, PROJECT2)).mkdir();
    for (val file : files.keySet()) {
      val lines = files.get(file);
      List<String> sortedLines = newArrayList();
      sortedLines.add(lines.get(0)); // add header
      sortedLines.addAll(Collections3.sort(lines.subList(1, lines.size())));
      if (file instanceof FileType) {
        val fileType = (FileType) file;
        writeInputFile(sortedLines, PROJECT1, fileType.getHarmonizedOutputFileName());
        writeInputFile(sortedLines, PROJECT2, fileType.getHarmonizedOutputFileName());
      } else {
        writeReferenceFile(sortedLines, file.getId());
      }
    }
  }

  private static Map<Identifiable, List<String>> getData(@NonNull final List<String> lines) {
    int fileNumber = 0;
    Identifiable currentFileType = null;
    List<String> currentLineList = null;
    Map<Identifiable, List<String>> files = newLinkedHashMap();
    for (val line : lines) {
      if (line.startsWith(FILE_SEPARATOR)) {
        currentFileType = FILES.get(fileNumber++);
        currentLineList = new ArrayList<String>();
        files.put(currentFileType, currentLineList);
      } else {
        if (!(line.isEmpty() || line.startsWith(HASHTAG))) {
          currentLineList.add(line);
        }
      }
    }

    return files;
  }

  @Test
  public void test_reporter() {
    val projectKeys = ImmutableSet.of(PROJECT1, PROJECT2);

    val outputDirPath = Reporter.report(
        TEST_RELEASE_NAME,
        Optionals.of(projectKeys),
        tempDir.getAbsolutePath(),
        PATH.join(TEST_CONF_DIR, PROJECTS_JSON_FILE_NAME),
        getDictionaryDccResource(),
        getCodeListsDccResource(),
        CascadingContext
            .getLocal()
            .getConnectors()
            .getDefaultProperties());

    val fileSystem = getDefaultLocalFileSystem();
    log.info("Content for '{}': '{}'", formatPrettyJson(
        ReporterCollector.getJsonProjectDataTypeEntity(
            fileSystem, outputDirPath, TEST_RELEASE_NAME)));
    log.info("Content for '{}': '{}'", formatPrettyJson(
        ReporterCollector.getJsonProjectSequencingStrategy(
            fileSystem, outputDirPath, TEST_RELEASE_NAME, EMPTY_MAP)));

    compare(outputDirPath, PRE_COMPUTATION);
    compare(outputDirPath, DONOR);
    compare(outputDirPath, SEQUENCING_STRATEGY);
  }

  private void compare(
      @NonNull final String outputDirPath,
      @NonNull final OutputType outputType) {
    val outputFileName = EXTENSION.join(DOT.join(outputType.getId(), TEST_RELEASE_NAME), Extensions.TSV);
    val outputFile = new File(PATH.join(outputDirPath, outputFileName));
    val referenceFile = new File(PATH.join(tempDir, outputType.getId()));
    val outputLines = Collections3.sort(readLines(outputFile));
    val referenceLines = Collections3.sort(readLines(referenceFile));

    log.info("\n\n" + NEWLINE.join(outputLines) + "\n");

    assertThat(NEWLINE.join(outputLines))
        .isEqualTo(NEWLINE.join(referenceLines));
  }

  @SneakyThrows
  private static List<String> readLines(@NonNull final File file) {
    return Files.readLines(file, Charsets.UTF_8);
  }

  @SneakyThrows
  private void writeInputFile(
      @NonNull final List<String> sortedLines,
      @NonNull final String projectKey,
      @NonNull final String fileName) {
    Files.write(
        NEWLINE.join(sortedLines).getBytes(),
        new File(PATH.join(tempDir, projectKey, fileName)));
  }

  @SneakyThrows
  private void writeReferenceFile(
      @NonNull final List<String> sortedLines,
      @NonNull final String fileName) {
    Files.write(
        NEWLINE.join(sortedLines).getBytes(),
        new File(PATH.join(tempDir, fileName)));
  }

}
