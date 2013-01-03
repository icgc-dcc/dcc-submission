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

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.icgc.dcc.integration.TestUtils.asDetailedSubmission;
import static org.icgc.dcc.integration.TestUtils.get;
import static org.icgc.dcc.integration.TestUtils.post;
import static org.icgc.dcc.integration.TestUtils.put;
import static org.icgc.dcc.integration.TestUtils.resourceToJsonArray;
import static org.icgc.dcc.integration.TestUtils.resourceToString;
import static org.icgc.dcc.release.model.SubmissionState.QUEUED;
import static org.icgc.dcc.release.model.SubmissionState.VALID;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.ws.rs.core.Response;

import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.loader.Main;
import org.icgc.dcc.release.model.DetailedSubmission;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.wordnik.system.mongodb.SnapshotUtil;

/**
 * Integration test to exercise the loader main entry point.
 * 
 * This test should be simplified to not use the Submitter to seed the data, but rather by a "back door". This will make
 * setup simpler and quicker.
 */
public class LoaderIntegrationTest extends BaseIntegrationTest {

  /**
   * Configuration file. Change this to switch environments.
   */
  private static final String ENV = "local"; // local, dev, prod

  /**
   * Test metadata constants.
   */
  // @formatter:off
  private static final int RELEASE_ID = 3;
  private static final String RELEASE_NAME = "release" + RELEASE_ID;
  private static final String PROJECT_NAME = "project1";
  private static final String RELEASE = "{\"name\":\"" + RELEASE_NAME + "\", \"state\":\"OPENED\",\"submissions\":[{\"projectKey\":\"" + PROJECT_NAME + "\",\"state\":\"NOT_VALIDATED\"}],\"dictionaryVersion\":\"0.6c\"}";
  private static final String PROJECT = "{\"name\":\"Project One\",\"key\":\"" + PROJECT_NAME + "\",\"users\":[\"admin\"],\"groups\":[\"admin\"]}";
  private static final String PROJECT_TO_SIGN_OFF = "[\"" + PROJECT_NAME + "\"]";
  private static final String PROJECTS_TO_ENQUEUE = "[{\"key\": \"" + PROJECT_NAME + "\", \"emails\": [\"a@a.ca\"]}]";
  private static final String NEXT_RELEASE = "{\"name\": \"release" + (RELEASE_ID + 1) + "\"}";
  // @formatter:on

  /**
   * REST endpoint constants.
   */
  // @formatter:off
  private static final String SEED_ENDPOINT = "/seed";
  private static final String SEED_CODELIST_ENDPOINT = SEED_ENDPOINT + "/codelists";
  private static final String SEED_DICTIONARIES_ENDPOINT = SEED_ENDPOINT + "/dictionaries";
  private static final String PROJECTS_ENDPOINT = "/projects";
  private static final String RELEASES_ENDPOINT = "/releases";
  private static final String NEXT_RELEASE_ENPOINT = "/nextRelease";
  private static final String SIGNOFF_ENDPOINT = NEXT_RELEASE_ENPOINT + "/signed";
  private static final String QUEUE_ENDPOINT = NEXT_RELEASE_ENPOINT + "/queue";
  private static final String RELEASE_ENDPOINT = RELEASES_ENDPOINT + "/" + RELEASE_NAME;
  private static final String RELEASE_SUBMISSIONS_ENDPOINT = RELEASE_ENDPOINT + "/submissions";
  // @formatter:on

  /**
   * Resource constants.
   */
  // @formatter:off
  private static final String INTEGRATION_TEST_RESOURCE_DIR = "/loader-integration-test";
  private static final String CODELISTS_RESOURCE = INTEGRATION_TEST_RESOURCE_DIR + "/codelists.json";
  private static final String DICTIONARY_RESOURCE = INTEGRATION_TEST_RESOURCE_DIR + "/dictionary.json";
  // @formatter:on

  /**
   * File system constants.
   */
  // @formatter:off
  private static final String INTEGRATION_TEST_DIR = "src/test/resources/loader-integration-test";
  private static final String FS_DIR = INTEGRATION_TEST_DIR + "/fs";
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

    // Basic sequence to initialize and validate a single project
    cleanStorage();
    startValidator();
    seedDb();
    createRelease();
    addProject();
    uploadSubmission();
    enqueueProject();
    validateSubmission();
    signOffProject();
    releaseRelease();
  }

  /**
   * Shuts down the validator / submitter threads.
   */
  @After
  public void teardown() {
    org.icgc.dcc.Main.shutdown();
  }

  /**
   * Execute the loader on an integration test data set and compares the results to verified set of output files.
   */
  @Test
  public void testLoader() {
    String[] args = { ENV, RELEASE_NAME };
    Main.main(args);

    verifyDb();
  }

  /**
   * Starts the validator web service.
   * 
   * @throws IOException
   */
  private void startValidator() throws IOException {
    String[] args = { ENV };
    org.icgc.dcc.Main.main(args);
  }

  /**
   * Seeds reference data.
   * 
   * @throws IOException
   */
  private void seedDb() throws IOException {
    post(client, SEED_DICTIONARIES_ENDPOINT, resourceToJsonArray(DICTIONARY_RESOURCE));
    post(client, SEED_CODELIST_ENDPOINT, resourceToString(CODELISTS_RESOURCE));
  }

  /**
   * Creates a release.
   * 
   * @throws Exception
   */
  private void createRelease() throws Exception {
    Response response = put(client, RELEASES_ENDPOINT, RELEASE);
    assertEquals(200, response.getStatus());
  }

  /**
   * Adds a new project.
   */
  private void addProject() {
    Response response = post(client, PROJECTS_ENDPOINT, PROJECT);
    assertEquals(201, response.getStatus());
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
   * Queue the added project.
   * 
   * @throws Exception
   */
  private void enqueueProject() throws Exception {
    Response response = get(client, QUEUE_ENDPOINT);
    assertEquals(200, response.getStatus());

    response = post(client, QUEUE_ENDPOINT, PROJECTS_TO_ENQUEUE);
    assertEquals(200, response.getStatus());
  }

  /**
   * Validates the queued project.
   * 
   * @throws Exception
   */
  private void validateSubmission() throws Exception {
    DetailedSubmission detailedSubmission;
    do {
      sleepUninterruptibly(2, SECONDS);

      Response response = get(client, RELEASE_SUBMISSIONS_ENDPOINT + "/" + PROJECT_NAME);
      assertEquals(200, response.getStatus());

      detailedSubmission = asDetailedSubmission(response);
    } while(detailedSubmission.getState() == QUEUED);

    assertEquals(VALID, detailedSubmission.getState());
  }

  /**
   * Signs off the validated project
   */
  private void signOffProject() {
    Response response = post(client, SIGNOFF_ENDPOINT, PROJECT_TO_SIGN_OFF);
    assertEquals(200, response.getStatus());
  }

  /**
   * Releases the signed-off project.
   */
  private void releaseRelease() {
    Response response = post(client, NEXT_RELEASE_ENPOINT, NEXT_RELEASE);
    assertEquals(200, response.getStatus());
  }

  /**
   * Verifies the integration database against of manually validated reference files.
   * 
   * @throws IOException
   */
  private void verifyDb() {
    exportDb(Main.DATABASE_NAME, getRootDir());

    for(File expectedFile : new File(MONGO_EXPORT_DIR).listFiles()) {
      File actualFile = new File(getRootDir(), expectedFile.getName());

      assertJsonFileEquals(expectedFile, actualFile);
    }
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
