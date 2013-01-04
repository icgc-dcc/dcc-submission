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
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.loader.Main;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
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
  // NOTE: To test against HDFS:
  // - set ENV to dev
  // - change application_dev.conf to use "mongodb://10.0.3.154";
  // - comment out the test body
  // - restore ENV to local
  // - mvn package -DskipTests=true
  // - cd target
  // - java -Xmx1g -cp dcc-server-1.5.jar org.icgc.dcc.loader.Main dev release3
  // - view job status at http://hcn51.res.oicr.on.ca:50030/
  private static final String ENV = "local"; // local, dev, prod

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

  protected String getValidatorDbName() {
    return new MongoURI(config.getString("mongo.uri")).getDatabase();
  }

  protected String getLoaderDbName() {
    return "icgc-loader-" + RELEASE_NAME;
  }

  protected String getLoaderExportDir() {
    return getRootDir() + "/" + "loader-export";
  }

  /**
   * Perform a clean release with a single validated project
   * 
   * @throws Exception
   */
  @Before
  public void setup() throws Exception {
    super.setup(Main.CONFIG.valueOf(ENV).filename);

    // Basic sequence to initialize and validate a single project
    cleanStorage();
    importDb(getValidatorDbName(), MONGO_IMPORT_DIR);
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

  /**
   * @throws IOException
   */
  protected void cleanStorage() throws IOException {
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

  /**
   * Asserts semantic JSON equality between {@code expectedFile} and {@code actualFile} using a memory efficient
   * stream-based comparison of deserialized sequences of JSON objects, ignoring transient fields.
   * 
   * @param expectedFile
   * @param actualFile
   */
  private static void assertJsonFileEquals(File expectedFile, File actualFile) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      MappingIterator<JsonNode> expected = mapper.reader(JsonNode.class).readValues(expectedFile);
      MappingIterator<JsonNode> actual = mapper.reader(JsonNode.class).readValues(actualFile);

      while(actual.hasNext() && expected.hasNext()) {
        JsonNode expectedJsonNode = expected.nextValue();
        JsonNode actualJsonNode = actual.nextValue();

        // Remove transient fields
        normalizeJsonNode(expectedJsonNode);
        normalizeJsonNode(actualJsonNode);

        assertEquals(
            "JSON mismatch between expected JSON file " + expectedFile + " and actual JSON file " + actualFile,
            expectedJsonNode, actualJsonNode);
      }

      // Ensure same number of elements
      assertEquals("Actual JSON file is missing objects", expected.hasNext(), false);
      assertEquals("Actual JSON file has additional objects", actual.hasNext(), false);
    } catch(IOException e) {
      Throwables.propagate(e);
    }
  }

  /**
   * Removes transient JSON properties that can change across runs (e.g. $oid).
   * 
   * @param jsonNode
   */
  private static void normalizeJsonNode(JsonNode jsonNode) {
    JsonUtils.filterTree(jsonNode, null, ImmutableList.of("$oid"), Integer.MAX_VALUE);
  }

}
