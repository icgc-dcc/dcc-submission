/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.integration;

import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.integration.JsonUtils.assertJsonFileEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.loader.Main;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Throwables;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.wordnik.system.mongodb.RestoreUtil;
import com.wordnik.system.mongodb.SnapshotUtil;

/**
 * Integration test to exercise the loader main entry point. This supports running in both "local" and "dev" modes.
 */
public class LoaderIntegrationTest extends BaseIntegrationTest {

  /**
   * Configuration file. Change this to switch environments.
   */
  // NOTE: To test against HDFS on the dev cluster:
  // 1. Copy data to HDFS:
  // - Set ENV to dev
  // - Comment out the test body
  // - Run this test
  // 2. Build jar and run cascade:
  // - Change application_dev.conf to use your machine ip (e.g. "mongodb://10.0.3.154") so that it can callback from the
  // cluster
  // - mvn package -DskipTests=true && cd target
  // - java -Xmx1g -cp dcc-server-1.5.jar org.icgc.dcc.loader.Main dev
  // release3
  // - View job status at http://hcn51.res.oicr.on.ca:50030/
  // 3. Cleanup:
  // - git checkout src/main/java/org/icgc/dcc/integration/LoaderIntegrationTest
  // - git checkout src/main/resources/application_dev.conf
  private static final String ENV = "local"; // One of {local, dev, qa}

  /**
   * Test metadata constants.
   */
  // @formatter:off
  private static final int RELEASE_ID = 3;
  private static final String RELEASE_NAME = "release" + RELEASE_ID;
  // @formatter:on

  /**
   * File system constants.
   */
  // @formatter:off
  private static final String INTEGRATION_TEST_DIR = "src/test/resources/loader-integration-test";
  private static final String FS_DIR = INTEGRATION_TEST_DIR + "/fs";
  private static final String MONGO_IMPORT_DIR = INTEGRATION_TEST_DIR + "/mongo-import";
  private static final String MONGO_EXPORT_DIR = INTEGRATION_TEST_DIR + "/mongo-export";
  private static final String SYSTEM_FILES_DIR = FS_DIR + "/SystemFiles";
  // @formatter:on

  /**
   * Perform a clean release with a single validated project
   * 
   * @throws Exception
   */
  @Before
  public void setup() throws Exception {
    super.setup(Main.CONFIG.valueOf(ENV).filename);

    cleanStorage();
    importValidatorDb();
    uploadSubmission();
  }

  /**
   * Execute the loader on an integration test data set and compares the results to verified set of output files.
   */
  @Test
  public void testLoader() {
    String[] args = { ENV, RELEASE_NAME };
    Main.main(args);

    exportLoaderDb();
    verifyLoaderDb();
  }

  // @formatter:on

  private String getValidatorDbName() {
    return new MongoURI(getMongoUri()).getDatabase();
  }

  private String getLoaderDbName() {
    return "icgc-loader-" + RELEASE_NAME;
  }

  private String getLoaderExportDir() {
    return getRootDir() + "/" + "loader-export";
  }

  /**
   * @throws IOException
   */
  private void cleanStorage() throws IOException {
    // Remove the root file system
    Path path = new Path(getRootDir());
    if(fs.exists(path)) {
      checkState(fs.delete(path, true));
    }

    // Drop test databases
    MongoURI uri = new MongoURI(getMongoUri());
    Mongo mongo = uri.connect();
    mongo.dropDatabase(getValidatorDbName());
    mongo.dropDatabase(getLoaderDbName());
  }

  /**
   * Imports the validator db state into the db.
   */
  private void importValidatorDb() {
    importDb(getValidatorDbName(), MONGO_IMPORT_DIR);
  }

  /**
   * Uploads a valid submission.
   * 
   * @throws IOException
   * @throws URISyntaxException
   */
  private void uploadSubmission() throws IOException, URISyntaxException {
    // Creating a release makes the dir again, so delete (hadoop fs requirement?)
    fs.delete(new Path(getRootDir() + "/" + RELEASE_NAME), true);

    fs.copyFromLocalFile(false, true, new Path(FS_DIR + "/" + RELEASE_NAME),
        new Path(getRootDir() + "/" + RELEASE_NAME));
    fs.copyFromLocalFile(false, true, new Path(SYSTEM_FILES_DIR), new Path(getRootDir() + "/" + RELEASE_NAME
        + "/SystemFiles"));
  }

  /**
   * Exports the loader database to the file system.
   */
  private void exportLoaderDb() {
    File loaderExportDir = new File(getLoaderExportDir());
    if(loaderExportDir.exists()) {
      try {
        FileUtils.deleteDirectory(loaderExportDir);
      } catch(IOException e) {
        Throwables.propagate(e);
      }
    }

    loaderExportDir.mkdirs();

    exportDb(getLoaderDbName(), getLoaderExportDir());
  }

  /**
   * Verifies the integration database against of manually validated reference files.
   * 
   * @throws IOException
   */
  private void verifyLoaderDb() {
    for(File expectedFile : new File(MONGO_EXPORT_DIR).listFiles()) {
      File actualFile = new File(getLoaderExportDir(), expectedFile.getName());
      JsonUtils.normalizeDumpFile(actualFile);

      System.out.println("Comparing: expected " + expectedFile + " to actual " + actualFile);
      assertJsonFileEquals(expectedFile, actualFile);
    }
  }

  /**
   * Import all collections in {@code dbName} to {@code inputDir} as serialized sequence files of JSON objects.
   * 
   * @param dbName
   * @param inputDir
   */
  private static void importDb(String dbName, String inputDir) {
    RestoreUtil.main("-d", dbName, "-i", inputDir, "-D");
  }

  /**
   * Export all collections in {@code dbName} to {@code outputDir} as serialized sequence files of JSON objects.
   * 
   * @param dbName
   * @param outputDir
   */
  private static void exportDb(String dbName, String outputDir) {
    SnapshotUtil.main("-d", dbName, "-o", outputDir, "-J");
  }

}
