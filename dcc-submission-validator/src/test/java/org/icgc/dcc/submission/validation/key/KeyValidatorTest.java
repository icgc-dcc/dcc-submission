/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.key;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.io.Files.readLines;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.util.Joiners.NEWLINE;
import static org.icgc.dcc.common.core.util.Joiners.PATH;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.lsRecursive;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.readSmallTextFile;
import static org.icgc.dcc.submission.dictionary.util.Dictionaries.readDccResourcesDictionary;
import static org.icgc.dcc.submission.fs.DccFileSystem.VALIDATION_DIRNAME;
import static org.icgc.dcc.submission.fs.ReleaseFileSystem.SYSTEM_FILES_DIR_NAME;
import static org.icgc.dcc.submission.validation.key.KVTestUtils.FS_DIR;
import static org.icgc.dcc.submission.validation.key.KVTestUtils.REFERENCE_FILE_NAME;
import static org.icgc.dcc.submission.validation.key.KVTestUtils.TEST_DIR;
import static org.icgc.dcc.submission.validation.key.KVTestUtils.copyDirectory;
import static org.icgc.dcc.submission.validation.key.report.KVReporter.REPORT_FILE_NAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.cascading.CascadingContext;
import org.icgc.dcc.common.core.model.DataType.DataTypes;
import org.icgc.dcc.common.core.util.Joiners;
import org.icgc.dcc.common.hadoop.fs.FileSystems;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Splitter;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class KeyValidatorTest {

  private static final String RELEASE_NAME = "myrelease";
  private static final String PROJECT_NAME = "myproject";

  private static final CascadingContext cascadingContext = CascadingContext.getLocal();

  /**
   * Scratch space.
   */
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private final FileSystem fileSystem = FileSystems.getDefaultLocalFileSystem();

  /**
   * Class under test.
   */
  private KeyValidator validator;
  private String validationDir;

  @Before
  public void setUp() {
    this.validator = new KeyValidator();
  }

  @Test
  public void testValidate() throws InterruptedException, IOException {
    val context = mockContext();
    validator.validate(context);

    String actualErrorLines = getActualErrorLines();
    String expectedErrorLines = getExpectedErrorLines();
    assertThat(actualErrorLines).isEqualTo(expectedErrorLines);
  }

  private ValidationContext mockContext() throws IOException {

    // Setup: Establish input for the test
    val rootDir = new Path(tmp.newFolder().getAbsolutePath());
    copyDirectory(
        fileSystem,
        new File(FS_DIR, PROJECT_NAME),
        new Path(new Path(rootDir, RELEASE_NAME), PROJECT_NAME));
    copyDirectory(
        fileSystem,
        new File(FS_DIR, SYSTEM_FILES_DIR_NAME),
        new Path(new Path(rootDir, RELEASE_NAME), SYSTEM_FILES_DIR_NAME));
    log.info("ls:\n\n\t{}\n", Joiners.INDENT.join(lsRecursive(fileSystem, rootDir)));

    validationDir = new Path(rootDir, VALIDATION_DIRNAME).toUri().toString();

    // Setup: Mock
    val release = mock(Release.class);
    when(release.getName()).thenReturn(RELEASE_NAME);

    val dictionary = readDccResourcesDictionary();

    val releaseFileSystem = mock(ReleaseFileSystem.class);

    val submissionDirectory = mock(SubmissionDirectory.class);
    when(submissionDirectory.getValidationDirPath()).thenReturn(validationDir);
    when(submissionDirectory.getSubmissionDirPath()).thenReturn(
        PATH.join(rootDir.toUri().toString(), RELEASE_NAME, PROJECT_NAME));
    when(submissionDirectory.getSystemDirPath()).thenReturn(
        PATH.join(rootDir.toUri().toString(), RELEASE_NAME, SYSTEM_FILES_DIR_NAME));

    val platformStrategy = mock(SubmissionPlatformStrategy.class);
    when(platformStrategy.getFlowConnector()).thenReturn(
        cascadingContext.getConnectors().getTestFlowConnector());

    val context = mock(ValidationContext.class);
    when(context.getProjectKey()).thenReturn("project1");
    when(context.getDataTypes()).thenReturn(DataTypes.values());
    when(context.getFileSystem()).thenReturn(fileSystem);
    when(context.getRelease()).thenReturn(release);
    when(context.getProjectKey()).thenReturn("project1");
    when(context.getReleaseFileSystem()).thenReturn(releaseFileSystem);
    when(context.getSubmissionDirectory()).thenReturn(submissionDirectory);
    when(context.getPlatformStrategy()).thenReturn(platformStrategy);
    when(context.getDictionary()).thenReturn(dictionary);

    return context;
  }

  private String getActualErrorLines() {
    val actualErrorLines = readSmallTextFile(fileSystem, new Path(validationDir, REPORT_FILE_NAME));
    checkState(actualErrorLines.size() == 1, "Expected to be all one line at the moment (may change later)");
    return NEWLINE.join(Splitter.on("}{").split(actualErrorLines.get(0)));
  }

  @SneakyThrows
  private String getExpectedErrorLines() {
    return NEWLINE.join(
        readLines(
            new File(PATH.join(TEST_DIR, REFERENCE_FILE_NAME)), UTF_8))
        .replace("}\n{", "\n"); // FIXME: not elegant (ideally tuple errors wouldn't all be on one line)
  }

}
