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

import static com.google.common.collect.Maps.newLinkedHashMap;
import static org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType.DONOR_TYPE;
import static org.icgc.dcc.submission.core.util.Joiners.PATH_JOINER;
import static org.icgc.dcc.submission.dictionary.util.Dictionaries.readDccResourcesDictionary;
import static org.icgc.dcc.submission.fs.DccFileSystem.VALIDATION_DIRNAME;
import static org.icgc.dcc.submission.validation.key.KVTestUtils.TEST_DIR;
import static org.icgc.dcc.submission.validation.key.KVTestUtils.copyDirectory;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import cascading.flow.local.LocalFlowConnector;

import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class KeyValidatorTest {

  private static final String RELEASE_NAME = "Release1";
  private static final String PROJECT_NAME = "project1";

  /**
   * Scratch space.
   */
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  /**
   * Class under test.
   */
  private KeyValidator validator;

  @Before
  public void setUp() {
    this.validator = new KeyValidator();
  }

  @Test
  public void testValidate() throws InterruptedException, IOException {
    val context = mockContext();
    validator.validate(context);
  }

  private ValidationContext mockContext() throws IOException {
    // Setup: Use local file system
    val fileSystem = FileSystem.getLocal(new Configuration());

    // Setup: Establish input for the test
    val rootDir = new Path(tmp.newFolder().getAbsolutePath());
    copyDirectory(
        fileSystem,
        new File(TEST_DIR),
        new Path(new Path(rootDir, RELEASE_NAME), PROJECT_NAME));

    // val path = new Path(directory, testFile.getName());
    val validationDir = new Path(rootDir, VALIDATION_DIRNAME).toUri().toString();

    // Setup: Mock
    val release = mock(Release.class);
    when(release.getName()).thenReturn(RELEASE_NAME);

    val dictionary = readDccResourcesDictionary();
    dictionary
        .getFileSchema(DONOR_TYPE)
        .setPattern("^donor\\.[0-9]+\\.txt(?:\\.gz|\\.bz2)?$");

    val submissionDirectory = mock(SubmissionDirectory.class);
    when(submissionDirectory.getValidationDirPath()).thenReturn(validationDir);
    when(submissionDirectory.getSubmissionDirPath()).thenReturn(
        PATH_JOINER.join(rootDir.toUri().toString(), RELEASE_NAME, PROJECT_NAME));

    val platformStrategy = mock(PlatformStrategy.class);
    val flowConnectorProperties = newLinkedHashMap(new ImmutableMap.Builder<Object, Object>()
        .put("fs.defaultFS", "file:///")
        .put("mapred.job.tracker", "")
        .build());
    val flowConnector = new LocalFlowConnector(flowConnectorProperties);
    when(platformStrategy.getFlowConnector()).thenReturn(flowConnector);

    val context = mock(ValidationContext.class);
    when(context.getFileSystem()).thenReturn(fileSystem);
    when(context.getRelease()).thenReturn(release);
    when(context.getProjectKey()).thenReturn("project1");
    when(context.getSubmissionDirectory()).thenReturn(submissionDirectory);
    when(context.getPlatformStrategy()).thenReturn(platformStrategy);
    when(context.getDictionary()).thenReturn(dictionary);

    return context;
  }
}
