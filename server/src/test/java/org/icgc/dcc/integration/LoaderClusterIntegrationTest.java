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
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.loader.Main;
import org.junit.Test;

import com.google.common.base.Throwables;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.wordnik.system.mongodb.RestoreUtil;
import com.wordnik.system.mongodb.SnapshotUtil;

/**
 * Integration test to exercise the loader main entry point using a pseudo-distributed cluster.
 */
public class LoaderClusterIntegrationTest extends BaseClusterTest {

  /**
   * Configuration file.
   */
  private static final String ENV = "local";

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

  private Config config;

  private FileSystem fs;

  /**
   * Simulates the state of a clean release with a single validated project
   * 
   * @throws Exception
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();

    configure(Main.CONFIG.valueOf(ENV).filename);
    cleanStorage();
    importValidatorDb();
    uploadSubmission();
  }

  /**
   * Execute the loader on an integration test data set and compares the results to verified set of output files.
   */
  @Test
  public void testLoader() {
    Main.main(ENV, RELEASE_NAME);

    exportLoaderDb();
    verifyLoaderDb();
  }

  private String getValidatorDbName() {
    return new MongoURI(getMongoUri()).getDatabase();
  }

  private String getLoaderDbName() {
    return "icgc-loader-" + RELEASE_NAME;
  }

  private String getFsUrl() {
    return config.getString("fs.url");
  }

  private String getRootDir() {
    return config.getString("fs.root");
  }

  private String getMongoUri() {
    return config.getString("mongo.uri");
  }

  private String getLoaderExportDir() {
    return getRootDir() + "/" + "loader-export";
  }

  /**
   * Configures the test.
   * 
   * @param fileName
   * @throws IOException
   * @throws URISyntaxException
   * @throws InterruptedException
   */
  private void configure(String fileName) throws IOException, URISyntaxException, InterruptedException {
    this.config = ConfigFactory.load(fileName);
    this.fs = FileSystem.get(new URI(getFsUrl()), new Configuration());
  }

  /**
   * Cleans persistent storage.
   * 
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
   * Verifies the integration database against of manually validated reference files.
   * 
   * @throws IOException
   */
  private void verifyLoaderDb() {
    for(File expectedFile : new File(MONGO_EXPORT_DIR).listFiles()) {
      File actualFile = new File(getLoaderExportDir(), expectedFile.getName());
      JsonUtils.normalizeDumpFile(actualFile);

      System.out.println("Comparing: expected = " + expectedFile + " to actual = " + actualFile);
      assertJsonFileEquals(expectedFile, actualFile);
    }
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
   * Import all collections in {@code inputDir} to {@code dbName} as serialized sequence files of JSON objects.
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
