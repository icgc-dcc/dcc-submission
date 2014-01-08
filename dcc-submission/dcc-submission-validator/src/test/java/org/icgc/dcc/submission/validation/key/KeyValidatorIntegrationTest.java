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

import java.io.File;
import java.io.IOException;

import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class KeyValidatorIntegrationTest {

  /**
   * Test file system.
   */
  static final File TEST_DIR = new File("src/test/resources/fixtures/validation/key/fs");
  static final File EXISTING_RELEASE_DIR = new File(TEST_DIR, "existing");
  static final File INCREMENTAL_RELEASE_DIR = new File(TEST_DIR, "incremental");

  /**
   * Test data.
   */
  static final String EXISTING_RELEASE_NAME = "release1";
  static final String INCREMENTAL_RELEASE_NAME = "release2";
  static final String PROJECT_KEY = "project1";

  /**
   * Scratch space.
   */
  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  /**
   * Environment.
   */
  FileSystem fileSystem;
  Path rootDir;

  /**
   * Class under test.
   */
  KeyValidator validator;

  @Before
  public void setUp() throws IOException {
    this.validator = new KeyValidator();
    this.fileSystem = FileSystem.getLocal(new Configuration());
    this.rootDir = new Path(tmp.newFolder().getAbsolutePath());
    System.out.println("Test root dir: '" + rootDir + "'");

    copyDirectory(EXISTING_RELEASE_DIR, new Path(new Path(rootDir, EXISTING_RELEASE_NAME), PROJECT_KEY));
    copyDirectory(INCREMENTAL_RELEASE_DIR, new Path(new Path(rootDir, INCREMENTAL_RELEASE_NAME), PROJECT_KEY));
  }

  @Test
  public void testValidate() throws InterruptedException {
    val context = createContext();

    validator.validate(context);
  }

  private void copyDirectory(File sourceDir, Path targetDir) throws IOException {
    for (val file : sourceDir.listFiles()) {
      val source = new Path(file.toURI());
      val target = new Path(targetDir, file.getName());

      System.out.println("Copying file: from '" + source + "' to '" + target + "'");
      fileSystem.copyFromLocalFile(source, target);
    }
  }

  private KeyValidationContext createContext() {
    val fsRoot = rootDir.toUri().toString();
    val fsUrl = fileSystem.getUri().toString();

    return new KeyValidationContext(EXISTING_RELEASE_NAME, INCREMENTAL_RELEASE_NAME, PROJECT_KEY, fsRoot, fsUrl);
  }

}