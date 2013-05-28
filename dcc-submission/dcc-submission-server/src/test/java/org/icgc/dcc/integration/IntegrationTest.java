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
package org.icgc.dcc.integration;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.icgc.dcc.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.release.model.SubmissionState.NOT_VALIDATED;
import static org.icgc.dcc.release.model.SubmissionState.QUEUED;
import static org.icgc.dcc.release.model.SubmissionState.VALID;
import static org.icgc.dcc.release.model.SubmissionState.VALIDATING;
import static org.icgc.dcc.validation.BaseCascadingStrategy.SEPARATOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
import org.icgc.dcc.sftp.SftpModule;
import org.icgc.dcc.shiro.ShiroModule;
import org.icgc.dcc.web.ServerErrorCode;
import org.icgc.dcc.web.WebModule;
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
@GuiceModules({ ConfigModule.class, CoreModule.class, HttpModule.class, JerseyModule.class, MorphiaModule.class, FileSystemModule.class, SftpModule.class, WebModule.class, ShiroModule.class })
public class IntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

  private static final String FIRST_DICTIONARY_VERSION = "0.6c";

  private static final String SECOND_DICTIONARY_VERSION = "0.6d";

  // ===========================================================================
  /*
   * If changing project names, must also change their directory counterparts under
   * server/src/test/resources/integrationtest/fs/release1
   */
  private static final String PROJECT1_NAME = "project.1";

  private static final String PROJECT2_NAME = "project.2";

  private static final String PROJECT3_NAME = "project.3";

  // ===========================================================================

  private static final String INITITAL_RELEASE_NAME = "release1";

  private static final String NEXT_RELEASE_NAME = "release2";

  private static final String SEED_ENDPOINT = "/seed";

  private static final String SEED_CODELIST_ENDPOINT = SEED_ENDPOINT + "/codelists";

  private static final String SEED_DICTIONARIES_ENDPOINT = SEED_ENDPOINT + "/dictionaries";

  private static final String DICTIONARIES_ENDPOINT = "/dictionaries";

  private static final String CODELISTS_ENDPOINT = "/codeLists";

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

  private static final String PROJECT1 = "{\"name\":\"Project One\",\"key\":\"" + PROJECT1_NAME
      + "\",\"users\":[\"admin\"],\"groups\":[\"admin\"]}"; // TODO: use:
                                                            // ./server/src/main/resources/integration/project1.json

  private static final String PROJECT2 = "{\"name\":\"Project Two\",\"key\":\"" + PROJECT2_NAME
      + "\",\"users\":[\"admin\", \"brett\"],\"groups\":[\"admin\"]}";

  private static final String PROJECT3 = "{\"name\":\"Project Three\",\"key\":\"" + PROJECT3_NAME
      + "\",\"users\":[\"admin\"],\"groups\":[\"admin\"]}";

  private static final String PROJECT_TO_SIGN_OFF = "[\"" + PROJECT1_NAME + "\"]";

  private static final String PROJECTS_TO_ENQUEUE = "[{\"key\": \"" + PROJECT1_NAME
      + "\", \"emails\": [\"a@a.ca\"]}, {\"key\": \"" + PROJECT2_NAME + "\", \"emails\": [\"a@a.ca\"]}, {\"key\": \""
      + PROJECT3_NAME + "\", \"emails\": [\"a@a.ca\"]}]";

  private static final String PROJECTS_TO_ENQUEUE2 = "[{\"key\": \"" + PROJECT2_NAME
      + "\", \"emails\": [\"a@a.ca\"]}, {\"key\": \"" + PROJECT3_NAME + "\", \"emails\": [\"a@a.ca\"]}]";

  private static final String FS_DIR = "src/test/resources/integrationtest/fs";

  private static final String SYSTEM_FILES_DIR = "src/test/resources/integrationtest/fs/SystemFiles";

  private static final String DCC_ROOT_DIR = ConfigFactory.load().getString("fs.root");

  private static final String INITIAL_RELEASE_SYSTEM_FILES_DIR = DCC_ROOT_DIR + "/release1/SystemFiles";

  private static final String PROJECT1_VALIDATION_DIR = "release1/" + PROJECT1_NAME + "/.validation";

  private final Client client = ClientFactory.newClient();

  private int dictionaryUpdateCount = 0;

  static {
    setProperties();
  }

  /**
   * Sets key system properties before test initialization.
   */
  private static void setProperties() {
    // See http://stackoverflow.com/questions/7134723/hadoop-on-osx-unable-to-load-realm-info-from-scdynamicstore
    System.setProperty("java.security.krb5.realm", "OX.AC.UK");
    System.setProperty("java.security.krb5.kdc", "kdc0.ox.ac.uk:kdc1.ox.ac.uk");
  }

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
    try {
      log.info("server main thread started");
      Main.main(null);
    } catch(Exception e) {
      e.printStackTrace();
    } finally {
      log.info("server main thread ended");
    }
  }

  @After
  public void stopServer() {
    Main.shutdown();
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
      updateDictionary( // dictionary is OPENED
          FIRST_DICTIONARY_RESOURCE, FIRST_DICTIONARY_VERSION, Status.NO_CONTENT.getStatusCode());
      enqueueProjects(PROJECTS_TO_ENQUEUE, Status.NO_CONTENT); // triggers validations
      checkValidations(); // will poll until all validated

      // Test that only NOT_VALIDATED projects can be enqueued
      enqueueProjects(PROJECTS_TO_ENQUEUE, Status.BAD_REQUEST);

      // Tests codelists
      addOffendingCodeLists();
      addValidCodeLists();
      addCodeListTerm();
      enqueueProjects(PROJECTS_TO_ENQUEUE2, Status.NO_CONTENT); // reenqueue them since they have been reset by adding
                                                                // the term
      Uninterruptibles.sleepUninterruptibly(30, TimeUnit.SECONDS);
      // TODO: make it such that adding a term fixed one of the submissions
      checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, ReleaseState.OPENED, //
          Arrays.<SubmissionState> asList( //
              SubmissionState.VALID, SubmissionState.INVALID, SubmissionState.INVALID));

      // release
      releaseInitialRelease();
      checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, ReleaseState.COMPLETED, //
          Arrays.<SubmissionState> asList(SubmissionState.SIGNED_OFF));
      checkRelease(NEXT_RELEASE_NAME, FIRST_DICTIONARY_VERSION, ReleaseState.OPENED, //
          Arrays.<SubmissionState> asList( //
              SubmissionState.NOT_VALIDATED, SubmissionState.INVALID, SubmissionState.INVALID));

      // update dictionaries
      updateDictionary( // dictionary is CLOSED
          FIRST_DICTIONARY_RESOURCE, FIRST_DICTIONARY_VERSION, Status.BAD_REQUEST.getStatusCode());
      updateDictionary( // dictionary is OPENED
          SECOND_DICTIONARY_RESOURCE, SECOND_DICTIONARY_VERSION, Status.NO_CONTENT.getStatusCode());

      // update release
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
    assertEquals(Status.OK.getStatusCode(), response.getStatus());

    Release release = TestUtils.asRelease(response);
    assertEquals(INITITAL_RELEASE_NAME, release.getName());
  }

  /**
   * TODO: improve this to make expectedSubmissionStates a map rather than a list (else order of project names could
   * break the test)
   * <p>
   * TODO: reuse checkValidatedSubmission() to while at it (since it's smarter and can poll)
   */
  private void checkRelease(String releaseName, String dictionaryVersion, ReleaseState expectedReleaseState,
      List<SubmissionState> expectedSubmissionStates) throws Exception {

    Response response = TestUtils.get(client, RELEASES_ENDPOINT + "/" + releaseName);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());

    ReleaseView releaseView = TestUtils.asReleaseView(response);
    assertNotNull(releaseView);

    assertEquals(dictionaryVersion, releaseView.getDictionaryVersion());
    assertEquals(expectedReleaseState, releaseView.getState());
    assertEquals(ImmutableList.<String> of(), releaseView.getQueue());
    assertEquals(expectedSubmissionStates.size(), releaseView.getSubmissions().size());
    int i = 0;
    for(DetailedSubmission submission : releaseView.getSubmissions()) {
      assertEquals(submission.getProjectKey(), expectedSubmissionStates.get(i++), submission.getState());
    }
  }

  private void addProjects() throws IOException {
    Response response1 = TestUtils.post(client, PROJECTS_ENDPOINT, PROJECT1);
    assertEquals(Status.CREATED.getStatusCode(), response1.getStatus());

    Response response2 = TestUtils.post(client, PROJECTS_ENDPOINT, PROJECT2);
    assertEquals(Status.CREATED.getStatusCode(), response2.getStatus());

    Response response3 = TestUtils.post(client, PROJECTS_ENDPOINT, PROJECT3);
    assertEquals(Status.CREATED.getStatusCode(), response3.getStatus());
  }

  private void enqueueProjects(String projectsToEnqueue, Status expectedStatus) throws Exception {
    Response response = TestUtils.get(client, QUEUE_ENDPOINT);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    assertEquals("[]", TestUtils.asString(response));

    response = TestUtils.post(client, QUEUE_ENDPOINT, projectsToEnqueue);
    assertEquals(expectedStatus.getStatusCode(), response.getStatus());
    if(expectedStatus != Status.NO_CONTENT) {
      assertEquals("{\"code\":\"" + ServerErrorCode.INVALID_STATE.getFrontEndString() + "\",\"parameters\":[\""
          + SubmissionState.VALID + "\"]}", TestUtils.asString(response));
    }
  }

  private void checkValidations() throws Exception {
    Response response = TestUtils.get(client, INITIAL_RELEASE_ENDPOINT);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());

    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT1_NAME, SubmissionState.VALID);
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT2_NAME, SubmissionState.INVALID);
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT3_NAME, SubmissionState.INVALID);

    // check no errors for project 1
    checkEmptyFile(DCC_ROOT_DIR, PROJECT1_VALIDATION_DIR + "/donor.internal" + SEPARATOR + "errors.json");
    checkEmptyFile(DCC_ROOT_DIR, PROJECT1_VALIDATION_DIR + "/specimen.internal" + SEPARATOR + "errors.json");
    checkEmptyFile(DCC_ROOT_DIR, PROJECT1_VALIDATION_DIR + "/specimen.external" + SEPARATOR + "errors.json");
    // TODO add more
  }

  private void addOffendingCodeLists() throws IOException {

    // Ensure codelist is present
    Response response = TestUtils.get(client, CODELISTS_ENDPOINT);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());
    final String CODELIST_NAME = "appendix_B10";
    assertTrue(TestUtils.asString(response).contains("\"" + CODELIST_NAME + "\""));

    // Attempt to add it again
    response =
        TestUtils.post(client, CODELISTS_ENDPOINT, "[{\"name\": \"someName\"}, {\"name\": \"" + CODELIST_NAME + "\"}]");
    assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  private void addValidCodeLists() throws IOException {
    Response response =
        TestUtils.post(client, CODELISTS_ENDPOINT, "[{\"name\": \"someName\"}, {\"name\": \"someNewName\"}]");
    assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
  }

  private void addCodeListTerm() throws Exception {
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED, asList(VALID, INVALID, INVALID));

    Response response = // 1: deceased, 2: alive
        TestUtils.post(client, CODELISTS_ENDPOINT + "/dr__donor_vital_status/terms",
            "[{\"code\": \"3\", \"value\": \"new value 1\"}, {\"code\": \"4\", \"value\": \"new value 2\"}]");
    assertEquals(CREATED.getStatusCode(), response.getStatus());

    // Only the INVALID ones should have been reset (DCC-851)
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED, asList(VALID, NOT_VALIDATED, NOT_VALIDATED));
  }

  private void checkValidatedSubmission(String release, String project, SubmissionState expectedSubmissionState)
      throws Exception {
    DetailedSubmission detailedSubmission;
    do {
      Uninterruptibles.sleepUninterruptibly(2, SECONDS);

      Response response = TestUtils.get(client, INITIAL_RELEASE_SUBMISSIONS_ENDPOINT + "/" + project);
      assertEquals(OK.getStatusCode(), response.getStatus());

      detailedSubmission = TestUtils.asDetailedSubmission(response);
    } while(detailedSubmission.getState() == QUEUED || detailedSubmission.getState() == VALIDATING);

    assertEquals(project, expectedSubmissionState, detailedSubmission.getState());
  }

  private void checkEmptyFile(String dir, String path) throws IOException {
    File errorFile = new File(dir, path);
    assertTrue("Expected file does not exist: " + path, errorFile.exists());
    assertTrue("Expected empty file: " + path, FileUtils.readFileToString(errorFile).isEmpty());
  }

  private void releaseInitialRelease() {
    // attempts releasing (expect failure)
    Response response = TestUtils.post(client, NEXT_RELEASE_ENPOINT, SECOND_RELEASE);
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus()); // no signed off projects

    // sign off
    response = TestUtils.post(client, SIGNOFF_ENDPOINT, PROJECT_TO_SIGN_OFF);
    assertEquals(Status.OK.getStatusCode(), response.getStatus());

    // attempt releasing again
    response = TestUtils.post(client, NEXT_RELEASE_ENPOINT, SECOND_RELEASE);
    assertEquals(TestUtils.asString(response), OK.getStatusCode(), response.getStatus());

    // attempt releasing one too many times
    response = TestUtils.post(client, NEXT_RELEASE_ENPOINT, SECOND_RELEASE);
    assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  private void updateDictionary(String dictionaryResource, String dictionaryVersion, int expectedStatus)
      throws Exception {
    String dictionary = TestUtils.resourceToString(dictionaryResource);
    String updatedSecondDictionary = dictionary.replace("Unique identifier for the donor", //
        "Unique identifier for the donor (update" + ++dictionaryUpdateCount + ")");
    assertTrue(dictionary, dictionary.equals(updatedSecondDictionary) == false);
    Response response = TestUtils.put(client, DICTIONARIES_ENDPOINT + "/" + dictionaryVersion, updatedSecondDictionary);
    assertEquals(expectedStatus, response.getStatus());
  }

  private void updateRelease(String updatedReleaseRelPath) throws Exception {
    Response response =
        TestUtils.put(client, UPDATE_RELEASE_ENDPOINT, TestUtils.resourceToString(updatedReleaseRelPath));
    assertEquals(Status.OK.getStatusCode(), response.getStatus());

    Release release = TestUtils.asRelease(response);
    assertEquals(NEXT_RELEASE_NAME, release.getName());
  }
}
