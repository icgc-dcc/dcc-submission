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
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.icgc.dcc.integration.TestUtils.asDetailedSubmission;
import static org.icgc.dcc.integration.TestUtils.asRelease;
import static org.icgc.dcc.integration.TestUtils.asString;
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

import org.icgc.dcc.loader.Main;
import org.icgc.dcc.release.model.DetailedSubmission;
import org.icgc.dcc.release.model.Release;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test to exercise the loader main entry point.
 * 
 * This test should be simplified to not use the Validator / Submitter to seed the data, but rather by a "back door".
 * This will make setup simpler.
 */
public class LoaderIntegrationTest extends BaseIntegrationTest {

  private static final String PROJECT_NAME = "project1";

  private static final String RELEASE_NAME = "release1";

  private static final String SEED_ENDPOINT = "/seed";

  private static final String SEED_CODELIST_ENDPOINT = SEED_ENDPOINT + "/codelists";

  private static final String SEED_DICTIONARIES_ENDPOINT = SEED_ENDPOINT + "/dictionaries";

  private static final String PROJECTS_ENDPOINT = "/projects";

  private static final String RELEASES_ENDPOINT = "/releases";

  private static final String NEXT_RELEASE_ENPOINT = "/nextRelease";

  private static final String NEXT_RELEASE = "{\"name\": \"release2\"}";

  private static final String SIGNOFF_ENDPOINT = NEXT_RELEASE_ENPOINT + "/signed";

  private static final String QUEUE_ENDPOINT = NEXT_RELEASE_ENPOINT + "/queue";

  private static final String RELEASE_ENDPOINT = RELEASES_ENDPOINT + "/" + RELEASE_NAME;

  private static final String RELEASE_SUBMISSIONS_ENDPOINT = RELEASE_ENDPOINT + "/submissions";

  private static final String INTEGRATION_TEST_DIR_RESOURCE = "/loader-integration-test";

  private static final String CODELISTS_RESOURCE = INTEGRATION_TEST_DIR_RESOURCE + "/codelists.json";

  private static final String DICTIONARY_RESOURCE = "/dictionary.json";

  private static final String RELEASE_RESOURCE = INTEGRATION_TEST_DIR_RESOURCE + "/initRelease.json";

  private static final String PROJECT =
      "{\"name\":\"Project One\",\"key\":\"project1\",\"users\":[\"admin\"],\"groups\":[\"admin\"]}";

  private static final String PROJECT_TO_SIGN_OFF = "[\"project1\"]";

  private static final String PROJECTS_TO_ENQUEUE = "[{\"key\": \"project1\", \"emails\": [\"a@a.ca\"]}]";

  private static final String FS_DIR = "src/test/resources/loader-integration-test/fs";

  private static final String SYSTEM_FILES_DIR = FS_DIR + "/SystemFiles";

  private static final String RELEASE_SYSTEM_FILES_DIR = DCC_ROOT_DIR + "/" + RELEASE_NAME + "/SystemFiles";

  /**
   * Perform a clean release with a single validated project
   * 
   * @throws Exception
   */
  @Before
  public void setup() throws Exception {
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
   * Execute loader on test data set.
   */
  @Test
  public void testSystem() {
    String[] args = { RELEASE_NAME };
    Main.main(args);
  }

  /**
   * @throws IOException
   */
  private void startValidator() throws IOException {
    String[] args = new String[] {};
    org.icgc.dcc.Main.main(args);
  }

  /**
   * @throws IOException
   */
  private void seedDb() throws IOException {
    post(client, SEED_DICTIONARIES_ENDPOINT, resourceToJsonArray(DICTIONARY_RESOURCE));
    post(client, SEED_CODELIST_ENDPOINT, resourceToString(CODELISTS_RESOURCE));
  }

  /**
   * @throws Exception
   */
  private void createRelease() throws Exception {
    Response response = put(client, RELEASES_ENDPOINT, resourceToString(RELEASE_RESOURCE));
    assertEquals(200, response.getStatus());

    Release release = asRelease(response);
    assertEquals(RELEASE_NAME, release.getName());
  }

  /**
   * @throws IOException
   */
  private void addProject() throws IOException {
    Response response = post(client, PROJECTS_ENDPOINT, PROJECT);
    assertEquals(201, response.getStatus());
  }

  /**
   * @throws IOException
   */
  private void uploadSubmission() throws IOException {
    copyDirectory(new File(FS_DIR), new File(DCC_ROOT_DIR));
    copyDirectory(new File(SYSTEM_FILES_DIR), new File(RELEASE_SYSTEM_FILES_DIR));
  }

  /**
   * @throws Exception
   */
  private void enqueueProject() throws Exception {
    Response response = get(client, QUEUE_ENDPOINT);
    assertEquals(200, response.getStatus());
    assertEquals("[]", asString(response));

    response = post(client, QUEUE_ENDPOINT, PROJECTS_TO_ENQUEUE);
    assertEquals(200, response.getStatus());
  }

  /**
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
   * 
   */
  private void signOffProject() {
    Response response = post(client, SIGNOFF_ENDPOINT, PROJECT_TO_SIGN_OFF);
    assertEquals(200, response.getStatus());
  }

  /**
   * 
   */
  private void releaseRelease() {
    Response response = post(client, NEXT_RELEASE_ENPOINT, NEXT_RELEASE);
    assertEquals(200, response.getStatus());
  }

}
