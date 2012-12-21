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

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.io.FileUtils.copyDirectory;
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

import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.loader.Main;
import org.icgc.dcc.release.model.DetailedSubmission;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.wordnik.system.mongodb.SnapshotUtil;

/**
 * Integration test to exercise the loader main entry point.
 * 
 * This test should be simplified to not use the Submitter to seed the data, but rather by a "back door". This will make
 * setup simpler and quicker.
 */
public class LoaderIntegrationTest extends BaseIntegrationTest {

  /**
   * Test metadata constants.
   */
  // @formatter:off
  private static final int RELEASE_ID = 2;
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
  private static final String RELEASE_SYSTEM_FILES_DIR = DCC_ROOT_DIR + "/" + RELEASE_NAME + "/SystemFiles";
  // @formatter:on

  /**
   * Perform a clean release with a single validated project
   * 
   * @throws Exception
   */
  @Before
  public void setup() throws Exception {
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
   * Execute the loader on an integration test data set and compares the results to verified set of output files.
   */
  @Test
  public void testSystem() {
    String[] args = { RELEASE_NAME };
    Main.main(args);

    verifyDb();
  }

  /**
   * Starts the validator web service.
   * 
   * @throws IOException
   */
  private void startValidator() throws IOException {
    String[] args = new String[] {};
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
   */
  private void uploadSubmission() throws IOException {
    copyDirectory(new File(FS_DIR), new File(DCC_ROOT_DIR));
    copyDirectory(new File(SYSTEM_FILES_DIR), new File(RELEASE_SYSTEM_FILES_DIR));
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
    // Export @formatter:off
   SnapshotUtil.main(new String[] {
      "-d", Main.DATABASE_NAME, // Database
      "-o", DCC_ROOT_DIR,       // Output dir
      "-J"                      // Output in json
   });
   // @formatter:on

    for(File file : new File(MONGO_EXPORT_DIR).listFiles()) {
      try {
        String expectedJson = Files.toString(file, UTF_8);
        String actualJson = Files.toString(new File(DCC_ROOT_DIR, file.getName()), UTF_8);

        assertJsonEquals(expectedJson, actualJson);
      } catch(IOException e) {
        Throwables.propagate(e);
      }
    }
  }

  /**
   * Asserts that {@code expectedJson} is canonically equal to {@code actualJson}.
   * 
   * @param actualJson
   * @param expectedJson
   */
  private static void assertJsonEquals(String actualJson, String expectedJson) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode actualJsonNode = mapper.readTree(actualJson);
      JsonNode expectedJsonNode = mapper.readTree(expectedJson);

      sanitizeJsonNode(actualJsonNode);
      sanitizeJsonNode(expectedJsonNode);

      assertEquals("JSON mismatch!", actualJsonNode, expectedJsonNode);
    } catch(Exception e) {
      Throwables.propagate(e);
    }
  }

  /**
   * Remove JSON properties that can change across runs (e.g. $oid).
   * 
   * @param jsonNode
   */
  private static void sanitizeJsonNode(JsonNode jsonNode) {
    JsonUtils.filterTree(jsonNode, null, ImmutableList.of("$oid"), Integer.MAX_VALUE);
  }

}
