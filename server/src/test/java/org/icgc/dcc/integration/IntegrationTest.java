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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.icgc.dcc.Main;
import org.icgc.dcc.config.ConfigModule;
import org.icgc.dcc.core.CoreModule;
import org.icgc.dcc.core.morphia.MorphiaModule;
import org.icgc.dcc.filesystem.FileSystemModule;
import org.icgc.dcc.filesystem.GuiceJUnitRunner;
import org.icgc.dcc.filesystem.GuiceJUnitRunner.GuiceModules;
import org.icgc.dcc.http.HttpModule;
import org.icgc.dcc.http.jersey.JerseyModule;
import org.icgc.dcc.release.model.DetailedSubmission;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.ReleaseView;
import org.icgc.dcc.release.model.SubmissionState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.morphia.Datastore;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Inject;
import com.typesafe.config.ConfigFactory;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ConfigModule.class, CoreModule.class, HttpModule.class, JerseyModule.class, MorphiaModule.class, FileSystemModule.class })
public class IntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

  private static final String FIRST_DICTIONARY_VERSION = "0.6c";

  private static final String SECOND_DICTIONARY_VERSION = "0.6d";

  private static final String PROJECT1_NAME = "project1";

  private static final String PROJECT2_NAME = "project2";

  private static final String PROJECT3_NAME = "project3";

  private static final String INITITAL_RELEASE_NAME = "release1";

  private static final String NEXT_RELEASE_NAME = "release2";

  private static final String SEED_ENDPOINT = "/seed";

  private static final String SEED_CODELIST_ENDPOINT = SEED_ENDPOINT + "/codelists";

  private static final String SEED_DICTIONARIES_ENDPOINT = SEED_ENDPOINT + "/dictionaries";

  private static final String DICTIONARIES_ENDPOINT = "/dictionaries";

  private static final String PROJECTS_ENDPOINT = "/projects";

  private static final String RELEASES_ENDPOINT = "/releases";

  private static final String NEXT_RELEASE_ENPOINT = "/nextRelease";

  private static final String UPDATE_RELEASE_ENDPOINT = NEXT_RELEASE_ENPOINT + "/update"; // TODO: ?

  private static final String SIGNOFF_ENDPOINT = NEXT_RELEASE_ENPOINT + "/signed";

  private static final String QUEUE_ENDPOINT = NEXT_RELEASE_ENPOINT + "/queue";

  private static final String INITIAL_RELEASE_ENDPOINT = RELEASES_ENDPOINT + "/" + INITITAL_RELEASE_NAME;

  private static final String INITIAL_RELEASE_SUBMISSIONS_ENDPOINT = INITIAL_RELEASE_ENDPOINT + "/submissions";

  private static final String INTEGRATION_TEST_DIR_RESOURCE = "/integrationtest";

  private static final String CODELISTS_RESOURCE = INTEGRATION_TEST_DIR_RESOURCE + "/codelists.json";

  private static final String FIRST_DICTIONARY_RESOURCE = "/dictionary.json";// TODO: move to INTEGRATION_TEST_DIR

  private static final String SECOND_DICTIONARY_RESOURCE = // careful, also updated by converter, do not edit manually,
                                                           // update via
                                                           // DictionaryConverterTest.updateSecondDictionaryContent()
                                                           // instead
      INTEGRATION_TEST_DIR_RESOURCE + "/secondDictionary.json";

  private static final String INITIAL_RELEASE_RESOURCE = INTEGRATION_TEST_DIR_RESOURCE + "/initRelease.json";

  private static final String UPDATED_INITIAL_RELEASE_RESOURCE = INTEGRATION_TEST_DIR_RESOURCE + "/updatedRelease.json";

  private static final String SECOND_RELEASE = "{\"name\": \"release2\"}";

  private static final String PROJECT1 =
      "{\"name\":\"Project One\",\"key\":\"project1\",\"users\":[\"admin\"],\"groups\":[\"admin\"]}";

  private static final String PROJECT2 =
      "{\"name\":\"Project Two\",\"key\":\"project2\",\"users\":[\"admin\", \"brett\"],\"groups\":[\"admin\"]}";

  private static final String PROJECT3 =
      "{\"name\":\"Project Three\",\"key\":\"project3\",\"users\":[\"admin\"],\"groups\":[\"admin\"]}";

  private static final String PROJECT_TO_SIGN_OFF = "[\"project1\"]";

  private static final String PROJECTS_TO_ENQUEUE =
      "[{\"key\": \"project1\", \"emails\": [\"a@a.ca\"]}, {\"key\": \"project2\", \"emails\": [\"a@a.ca\"]}, {\"key\": \"project3\", \"emails\": [\"a@a.ca\"]}]";

  private static final String FS_DIR = "src/test/resources/integrationtest/fs";

  private static final String SYSTEM_FILES_DIR = "src/test/resources/integrationtest/fs/SystemFiles";

  private static final String DCC_ROOT_DIR = ConfigFactory.load().getString("fs.root");

  private static final String INITIAL_RELEASE_SYSTEM_FILES_DIR = DCC_ROOT_DIR + "/release1/SystemFiles";

  private static final String PROJECT1_VALIDATION_DIR = "release1/project1/.validation";

  private final ExecutorService service = Executors.newSingleThreadExecutor();

  private final Client client = ClientFactory.newClient();

  @Inject
  private Datastore datastore;

  @Before
  public void startServer() throws IOException {

    // clean up fs
    FileUtils.deleteDirectory(new File(DCC_ROOT_DIR));

    // clean up db
    datastore.getDB().dropDatabase();

    // start server
    log.info("starting server");
    service.execute(new Runnable() {
      @Override
      public void run() {
        try {
          log.info("server main thread started");
          Main.main(null);
        } catch(Exception e) {
          e.printStackTrace();
        } finally {
          log.info("server main thread ended");
        }
      }
    });

    // give enough time for server to start properly before having the test connect to it
    Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
  }

  @After
  public void stopServer() {
    log.info("shutting down server");
    service.shutdown();
    log.info("shut down server");
  }

  @Test
  public void testSystem() throws Exception {
    log.info("starting tests");
    try {

      // feed db
      TestUtils.post(client, SEED_DICTIONARIES_ENDPOINT, TestUtils.resourceToJsonArray(FIRST_DICTIONARY_RESOURCE));
      TestUtils.post(client, SEED_DICTIONARIES_ENDPOINT, TestUtils.resourceToJsonArray(SECOND_DICTIONARY_RESOURCE));
      TestUtils.post(client, SEED_CODELIST_ENDPOINT, TestUtils.resourceToString(CODELISTS_RESOURCE));

      // create initial release and projects (and therefore submissions)
      createInitialRelease();
      checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, ReleaseState.OPENED, //
          Arrays.<SubmissionState> asList());
      addProjects();
      checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, ReleaseState.OPENED, //
          Arrays.<SubmissionState> asList( //
              SubmissionState.NOT_VALIDATED, SubmissionState.NOT_VALIDATED, SubmissionState.NOT_VALIDATED));

      // feed filesystem; TODO: ideally we should use an sftp client to upload data files
      FileUtils.copyDirectory(new File(FS_DIR), new File(DCC_ROOT_DIR));
      FileUtils.copyDirectory(new File(SYSTEM_FILES_DIR), new File(INITIAL_RELEASE_SYSTEM_FILES_DIR));

      // validate
      enqueueProjects(); // triggers validations
      checkValidations(); // will poll until all validated

      // release
      releaseInitialRelease();
      checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, ReleaseState.COMPLETED, //
          Arrays.<SubmissionState> asList(SubmissionState.SIGNED_OFF));
      checkRelease(NEXT_RELEASE_NAME, FIRST_DICTIONARY_VERSION, ReleaseState.OPENED, //
          Arrays.<SubmissionState> asList( //
              SubmissionState.NOT_VALIDATED, SubmissionState.INVALID, SubmissionState.INVALID));

      // update release with another dictionary (that we also update while at it)
      updateDictionary();
      updateRelease(UPDATED_INITIAL_RELEASE_RESOURCE);
      checkRelease(NEXT_RELEASE_NAME, SECOND_DICTIONARY_VERSION, ReleaseState.OPENED, //
          Arrays.<SubmissionState> asList( //
              SubmissionState.NOT_VALIDATED, SubmissionState.NOT_VALIDATED, SubmissionState.NOT_VALIDATED));

    } catch(Exception e) {
      e.printStackTrace(); // make sure we get stacktrace
      throw e;
    }

    log.info("ending tests");
  }

  private void createInitialRelease() throws Exception {
    Response response = TestUtils.put(client, RELEASES_ENDPOINT, TestUtils.resourceToString(INITIAL_RELEASE_RESOURCE));
    assertEquals(200, response.getStatus());

    Release release = TestUtils.asRelease(response);
    assertEquals(INITITAL_RELEASE_NAME, release.getName());
  }

  private void checkRelease(String releaseName, String dictionaryVersion, ReleaseState expectedReleaseState,
      List<SubmissionState> expectedSubmissionStates) throws Exception {

    Response response = TestUtils.get(client, RELEASES_ENDPOINT + "/" + releaseName);
    assertEquals(200, response.getStatus());

    ReleaseView releaseView = TestUtils.asReleaseView(response);
    assertNotNull(releaseView);

    assertEquals(dictionaryVersion, releaseView.getDictionaryVersion());
    assertEquals(expectedReleaseState, releaseView.getState());
    assertEquals(ImmutableList.<String> of(), releaseView.getQueue());
    assertEquals(expectedSubmissionStates.size(), releaseView.getSubmissions().size());
    int i = 0;
    for(DetailedSubmission submission : releaseView.getSubmissions()) {
      assertEquals(expectedSubmissionStates.get(i++), submission.getState());
    }
  }

  private void addProjects() throws IOException {
    Response response1 = TestUtils.post(client, PROJECTS_ENDPOINT, PROJECT1);
    assertEquals(201, response1.getStatus());

    Response response2 = TestUtils.post(client, PROJECTS_ENDPOINT, PROJECT2);
    assertEquals(201, response2.getStatus());

    Response response3 = TestUtils.post(client, PROJECTS_ENDPOINT, PROJECT3);
    assertEquals(201, response3.getStatus());
  }

  private void enqueueProjects() throws Exception {
    Response response = TestUtils.get(client, QUEUE_ENDPOINT);
    assertEquals(200, response.getStatus());
    assertEquals("[]", TestUtils.asString(response));

    response = TestUtils.post(client, QUEUE_ENDPOINT, PROJECTS_TO_ENQUEUE);
    assertEquals(200, response.getStatus());
  }

  private void checkValidations() throws Exception {
    Response response = TestUtils.get(client, INITIAL_RELEASE_ENDPOINT);
    assertEquals(200, response.getStatus());

    checkValidatedSubmission(PROJECT1_NAME, SubmissionState.VALID);
    checkValidatedSubmission(PROJECT2_NAME, SubmissionState.INVALID);
    checkValidatedSubmission(PROJECT3_NAME, SubmissionState.INVALID);

    // check no errors for project 1
    checkEmptyFile(DCC_ROOT_DIR, PROJECT1_VALIDATION_DIR + "/donor.internal#errors.json");
    checkEmptyFile(DCC_ROOT_DIR, PROJECT1_VALIDATION_DIR + "/specimen.internal#errors.json");
    checkEmptyFile(DCC_ROOT_DIR, PROJECT1_VALIDATION_DIR + "/specimen.external#errors.json");
    // TODO add more
  }

  private void checkValidatedSubmission(String project, SubmissionState expectedSubmissionState) throws Exception {
    DetailedSubmission detailedSubmission;
    do {
      Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);

      Response response = TestUtils.get(client, INITIAL_RELEASE_SUBMISSIONS_ENDPOINT + "/" + project);
      assertEquals(200, response.getStatus());

      detailedSubmission = TestUtils.asDetailedSubmission(response);
    } while(detailedSubmission.getState() == SubmissionState.QUEUED);

    assertEquals(expectedSubmissionState, detailedSubmission.getState());
  }

  private void checkEmptyFile(String dir, String path) throws IOException {
    File errorFile = new File(dir, path);
    assertTrue("Expected file does not exist: " + path, errorFile.exists());
    assertTrue("Expected empty file: " + path, FileUtils.readFileToString(errorFile).isEmpty());
  }

  private void releaseInitialRelease() {
    // attempts releasing (expect failure)
    Response response = TestUtils.post(client, NEXT_RELEASE_ENPOINT, SECOND_RELEASE);
    assertEquals(400, response.getStatus()); // no signed off projects

    // sign off
    response = TestUtils.post(client, SIGNOFF_ENDPOINT, PROJECT_TO_SIGN_OFF);
    assertEquals(200, response.getStatus());

    // attempt releasing again
    response = TestUtils.post(client, NEXT_RELEASE_ENPOINT, SECOND_RELEASE);
    assertEquals(200, response.getStatus());

    // attempt releasing one too many times
    response = TestUtils.post(client, NEXT_RELEASE_ENPOINT, SECOND_RELEASE);
    assertEquals(400, response.getStatus());
  }

  private void updateDictionary() throws Exception {
    String secondDictionary = TestUtils.resourceToString(SECOND_DICTIONARY_RESOURCE);
    String updatedSecondDictionary = secondDictionary.replace("Unique identifier for the donor", "Donor ID");
    assertTrue(secondDictionary.equals(updatedSecondDictionary) == false);
    Response response = TestUtils.put(client, DICTIONARIES_ENDPOINT + "/0.6d", updatedSecondDictionary);
    assertEquals(200, response.getStatus());
  }

  private void updateRelease(String updatedReleaseRelPath) throws Exception {
    Response response =
        TestUtils.put(client, UPDATE_RELEASE_ENDPOINT, TestUtils.resourceToString(updatedReleaseRelPath));
    assertEquals(200, response.getStatus());

    Release release = TestUtils.asRelease(response);
    assertEquals(NEXT_RELEASE_NAME, release.getName());
  }
}
