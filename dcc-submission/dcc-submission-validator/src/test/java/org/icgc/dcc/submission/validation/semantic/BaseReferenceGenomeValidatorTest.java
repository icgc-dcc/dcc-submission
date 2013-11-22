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
package org.icgc.dcc.submission.validation.semantic;

import static org.icgc.dcc.submission.fs.DccFileSystem.VALIDATION_DIRNAME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;

public class BaseReferenceGenomeValidatorTest {

  /**
   * Test data.
   */
  protected static final String TEST_DIR = "src/test/resources/fixtures/validation/rgv";
  protected static final Path TEST_FILE = new Path(TEST_DIR, "ssm_p.txt");

  /**
   * Scratch space.
   */
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  /**
   * Class under test.
   */
  protected ReferenceGenomeValidator validator;

  @Before
  public void setup() {
    validator = new ReferenceGenomeValidator("/tmp/GRCh37.fasta");
  }

  protected ValidationContext mockContext(Path testFile) throws IOException {
    // Setup: Use local file system
    val fileSystem = FileSystem.getLocal(new Configuration());

    // Setup: Establish input for the test
    val directory = new Path(tmp.newFolder().getAbsolutePath());
    val path = new Path(directory, testFile.getName());
    val ssmPrimaryFile = Optional.<Path> of(path);
    val validationDir = new Path(directory, VALIDATION_DIRNAME).toUri().toString();

    // Setup: Mock
    val context = mock(ValidationContext.class);
    val submissionDirectory = mock(SubmissionDirectory.class);
    when(context.getFileSystem()).thenReturn(fileSystem);
    when(context.getSsmPrimaryFile()).thenReturn(ssmPrimaryFile);
    when(context.getProjectKey()).thenReturn("project.test");
    when(context.getSubmissionDirectory()).thenReturn(submissionDirectory);
    when(submissionDirectory.getValidationDirPath()).thenReturn(validationDir);

    // Setup: "Submit" file
    fileSystem.createNewFile(directory);
    fileSystem.copyFromLocalFile(testFile, path);

    return context;
  }

}
