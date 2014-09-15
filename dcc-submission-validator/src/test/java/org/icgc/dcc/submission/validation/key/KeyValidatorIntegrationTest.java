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

import static org.icgc.dcc.hadoop.fs.FileSystems.getDefaultLocalFileSystem;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.lsRecursive;
import static org.icgc.dcc.submission.dictionary.util.Dictionaries.readDccResourcesDictionary;
import static org.icgc.dcc.submission.fs.ReleaseFileSystem.SYSTEM_FILES_DIR_NAME;
import static org.icgc.dcc.submission.validation.key.KVTestUtils.FS_DIR;
import static org.icgc.dcc.submission.validation.key.KVTestUtils.copyDirectory;

import java.io.File;
import java.io.IOException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.util.Joiners;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.validation.key.cli.KeyValidationContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@Slf4j
public class KeyValidatorIntegrationTest {

  /**
   * Test data.
   */
  static final String RELEASE_NAME = "myrelease";
  static final String PROJECT_KEY = "myproject";

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
    this.fileSystem = getDefaultLocalFileSystem();
    this.rootDir = new Path(tmp.newFolder().getAbsolutePath());
    log.info("Test root dir: '{}'", rootDir);

    copyDirectory(
        fileSystem,
        new File(FS_DIR, PROJECT_KEY),
        new Path(new Path(rootDir, RELEASE_NAME), PROJECT_KEY));
    copyDirectory(
        fileSystem,
        new File(FS_DIR, SYSTEM_FILES_DIR_NAME),
        new Path(new Path(rootDir, RELEASE_NAME), SYSTEM_FILES_DIR_NAME));
    log.info("ls:\n\n\t{}\n", Joiners.INDENT.join(lsRecursive(fileSystem, rootDir)));
  }

  @Test
  public void testValidate() throws InterruptedException {
    val context = createContext();

    validator.validate(context);
  }

  private KeyValidationContext createContext() {
    val fsRoot = rootDir.toUri().toString();
    val fsUrl = fileSystem.getUri().toString();
    val jobTracker = "localhost"; // Not used

    return new KeyValidationContext(
        RELEASE_NAME, PROJECT_KEY,
        fsRoot, fsUrl, jobTracker) {

      @Override
      protected Dictionary createDictionary() {
        return readDccResourcesDictionary();
      }

    };
  }

}