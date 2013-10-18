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
package org.icgc.dcc.submission;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.icgc.dcc.submission.TestUtils.CODELISTS_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.DICTIONARIES_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.NEXT_RELEASE_ENPOINT;
import static org.icgc.dcc.submission.TestUtils.PROJECTS_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.QUEUE_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.RELEASES_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.SEED_CODELIST_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.SEED_DICTIONARIES_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.SIGNOFF_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.UPDATE_RELEASE_ENDPOINT;
import static org.icgc.dcc.submission.TestUtils.asDetailedSubmission;
import static org.icgc.dcc.submission.TestUtils.asRelease;
import static org.icgc.dcc.submission.TestUtils.asReleaseView;
import static org.icgc.dcc.submission.TestUtils.asString;
import static org.icgc.dcc.submission.TestUtils.codeListsToString;
import static org.icgc.dcc.submission.TestUtils.dictionaryToString;
import static org.icgc.dcc.submission.TestUtils.get;
import static org.icgc.dcc.submission.TestUtils.post;
import static org.icgc.dcc.submission.TestUtils.put;
import static org.icgc.dcc.submission.TestUtils.resourceToJsonArray;
import static org.icgc.dcc.submission.TestUtils.resourceToString;
import static org.icgc.dcc.submission.release.model.ReleaseState.COMPLETED;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.NOT_VALIDATED;
import static org.icgc.dcc.submission.release.model.SubmissionState.QUEUED;
import static org.icgc.dcc.submission.release.model.SubmissionState.SIGNED_OFF;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALIDATING;
import static org.icgc.dcc.submission.validation.CascadingStrategy.SEPARATOR;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.INVALID_STATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.config.ConfigModule;
import org.icgc.dcc.submission.core.CoreModule;
import org.icgc.dcc.submission.core.morphia.MorphiaModule;
import org.icgc.dcc.submission.fs.FileSystemModule;
import org.icgc.dcc.submission.fs.GuiceJUnitRunner;
import org.icgc.dcc.submission.fs.GuiceJUnitRunner.GuiceModules;
import org.icgc.dcc.submission.http.HttpModule;
import org.icgc.dcc.submission.http.jersey.JerseyModule;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.ReleaseView;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.sftp.SftpModule;
import org.icgc.dcc.submission.shiro.ShiroModule;
import org.icgc.dcc.submission.web.WebModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.code.morphia.Datastore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.typesafe.config.ConfigFactory;

@Slf4j
@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ConfigModule.class, CoreModule.class, HttpModule.class, JerseyModule.class, MorphiaModule.class, FileSystemModule.class, SftpModule.class, WebModule.class, ShiroModule.class })
public class SubmissionIntegrationTest extends BaseIntegrationTest {

  /**
   * Dictionaries.
   */
  // TODO: Parse from files
  private static final String FIRST_DICTIONARY_VERSION = "0.6d";
  private static final String SECOND_DICTIONARY_VERSION = "0.6e";

  /**
   * Projects.
   * 
   * If changing project names, must also change their directory counterparts under
   * server/src/test/resources/fixtures/submission/fs/release1
   */
  private static final String PROJECT1_NAME = "project.1";
  private static final String PROJECT1 = "{\"name\":\"Project One\",\"key\":\"" + PROJECT1_NAME
      + "\",\"users\":[\"admin\"],\"groups\":[\"admin\"]}";
  private static final String PROJECT2_NAME = "project.2";
  private static final String PROJECT2 = "{\"name\":\"Project Two\",\"key\":\"" + PROJECT2_NAME
      + "\",\"users\":[\"admin\", \"brett\"],\"groups\":[\"admin\"]}";
  private static final String PROJECT3_NAME = "project.3";
  private static final String PROJECT3 = "{\"name\":\"Project Three\",\"key\":\"" + PROJECT3_NAME
      + "\",\"users\":[\"admin\"],\"groups\":[\"admin\"]}";

  /**
   * Releases.
   */
  private static final String INITITAL_RELEASE_NAME = "release1";
  private static final String INITIAL_RELEASE_ENDPOINT = RELEASES_ENDPOINT + "/" + INITITAL_RELEASE_NAME;
  private static final String INITIAL_RELEASE_SUBMISSIONS_ENDPOINT = INITIAL_RELEASE_ENDPOINT + "/submissions";

  private static final String NEXT_RELEASE_NAME = "release2";
  private static final String NEXT_RELEASE = "{\"name\": \"release2\"}";

  /**
   * Resources.
   */
  private static final String INTEGRATION_TEST_DIR_RESOURCE = "/fixtures/submission";
  private static final String FIRST_DICTIONARY_RESOURCE = INTEGRATION_TEST_DIR_RESOURCE + "/initDictionary.json";
  private static final String INITIAL_RELEASE_RESOURCE = INTEGRATION_TEST_DIR_RESOURCE + "/initRelease.json";
  private static final String UPDATED_INITIAL_RELEASE_RESOURCE = INTEGRATION_TEST_DIR_RESOURCE + "/updatedRelease.json";

  private static final String PROJECT_TO_SIGN_OFF = "[\"" + PROJECT1_NAME + "\"]";
  private static final String PROJECTS_TO_ENQUEUE = "[{\"key\": \"" + PROJECT1_NAME
      + "\", \"emails\": [\"a@a.ca\"]}, {\"key\": \"" + PROJECT2_NAME + "\", \"emails\": [\"a@a.ca\"]}, {\"key\": \""
      + PROJECT3_NAME + "\", \"emails\": [\"a@a.ca\"]}]";
  private static final String PROJECTS_TO_ENQUEUE2 = "[{\"key\": \"" + PROJECT2_NAME
      + "\", \"emails\": [\"a@a.ca\"]}, {\"key\": \"" + PROJECT3_NAME + "\", \"emails\": [\"a@a.ca\"]}]";

  private static final String FS_DIR = "src/test/resources/fixtures/submission/fs";
  private static final String SYSTEM_FILES_DIR = "src/test/resources/fixtures/submission/fs/SystemFiles";
  private static final String DCC_ROOT_DIR = ConfigFactory.load().getString("fs.root");

  private static final String INITIAL_RELEASE_SYSTEM_FILES_DIR = DCC_ROOT_DIR + "/release1/SystemFiles";
  private static final String PROJECT1_VALIDATION_DIR = "release1/" + PROJECT1_NAME + "/.validation";

  private int dictionaryUpdateCount = 0;

  @Inject
  private Datastore datastore;

  @Before
  public void startServer() throws IOException {
    log.info(repeat("-", 100));
    log.info("Submission Integration Test");
    log.info(repeat("-", 100));

    status("init", "Deleting filesystem...");
    deleteDirectory(new File(DCC_ROOT_DIR));

    status("init", "Dropping database...");
    datastore.getDB().dropDatabase();

    try {
      status("init", "Starting server...");
      Main.main(null);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      status("init", "Main thread ending...");
    }
  }

  @After
  public void stopServer() {
    status("shutdown", "Shutting down server...");
    Main.shutdown();
    status("shutdown", "Server shut down.");
  }

  @Test
  public void testSystem() throws Exception {
    status("test", "Starting test...");
    try {
      seedSystem();
      adminCreatesRelease();
      userSubmitsFiles();
      userValidates();
      adminTriesToValidate();
      adminTweaksCodeListAndTerms();
      adminRevalidates();
      adminPerformsRelease();
      adminUpdatesDictionary();
      adminUpdatesRelease();
    } catch (Exception e) {
      status("test", "Caught exception: " + e);
      throw e;
    }

    status("test", "Finished test.");
  }

  private void seedSystem() throws IOException {
    status("seed", "Seeding dictionary 1 ({})...", FIRST_DICTIONARY_VERSION);
    post(client, SEED_DICTIONARIES_ENDPOINT, resourceToJsonArray(FIRST_DICTIONARY_RESOURCE));

    status("seed", "Seeding dictionary 2 (dcc-resources)...");
    post(client, SEED_DICTIONARIES_ENDPOINT, "[" + dictionaryToString() + "]");

    status("seed", "Seeding code lists...");
    post(client, SEED_CODELIST_ENDPOINT, codeListsToString());

    status("seed", "Seeding system files...");
    copyDirectory(new File(SYSTEM_FILES_DIR), new File(INITIAL_RELEASE_SYSTEM_FILES_DIR));
  }

  private void adminCreatesRelease() throws Exception, IOException {
    status("admin", "Creating initial release...");
    createInitialRelease();
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        Lists.<SubmissionState> newArrayList());

    status("admin", "Adding projects...");
    addProjects();
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        newArrayList(NOT_VALIDATED, NOT_VALIDATED, NOT_VALIDATED));

    status("admin", "Updating dictionary...");
    updateDictionary( // dictionary is OPENED
        resourceToString(FIRST_DICTIONARY_RESOURCE), FIRST_DICTIONARY_VERSION, NO_CONTENT.getStatusCode());
  }

  private void userSubmitsFiles() throws IOException {
    status("user", "\"Submitting\" files...");
    copyDirectory(new File(FS_DIR), new File(DCC_ROOT_DIR));
  }

  private void userValidates() throws Exception {
    // Triggers validations
    enqueueProjects(PROJECTS_TO_ENQUEUE, NO_CONTENT);
    checkValidations();
  }

  private void adminTriesToValidate() throws Exception {
    // Test that only NOT_VALIDATED projects can be enqueued
    enqueueProjects(PROJECTS_TO_ENQUEUE, BAD_REQUEST);
  }

  private void adminTweaksCodeListAndTerms() throws IOException, Exception {
    status("admin", "Adding offending code list...");
    addOffendingCodeList();

    status("admin", "Adding valid code list...");
    addValidCodeLists();

    status("admin", "Adding code list term...");
    addCodeListTerm();
  }

  private void adminRevalidates() throws Exception {
    // Re-enqueue them since they have been reset by adding the term
    enqueueProjects(PROJECTS_TO_ENQUEUE2, NO_CONTENT);

    status("admin", "Checking validated submission 1...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT1_NAME, VALID);

    status("admin", "Checking validated submission 2...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT2_NAME, INVALID);

    status("admin", "Checking validated submission 3...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT2_NAME, INVALID);

    // TODO: make it such that adding a term fixed one of the submissions
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        newArrayList(VALID, INVALID, INVALID));
  }

  private void adminPerformsRelease() throws Exception {
    releaseInitialRelease();
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, COMPLETED,
        newArrayList(SIGNED_OFF));
    checkRelease(NEXT_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        newArrayList(NOT_VALIDATED, INVALID, INVALID));
  }

  private void adminUpdatesDictionary() throws Exception, IOException {
    // Try with CLOSED dictionary
    updateDictionary(
        resourceToString(FIRST_DICTIONARY_RESOURCE), FIRST_DICTIONARY_VERSION, BAD_REQUEST.getStatusCode());

    // Try with OPENED dictionary
    updateDictionary(
        dictionaryToString(), SECOND_DICTIONARY_VERSION, NO_CONTENT.getStatusCode());
  }

  private void adminUpdatesRelease() throws Exception {
    updateRelease(UPDATED_INITIAL_RELEASE_RESOURCE);
    checkRelease(NEXT_RELEASE_NAME, SECOND_DICTIONARY_VERSION, OPENED,
        newArrayList(NOT_VALIDATED, NOT_VALIDATED, NOT_VALIDATED));
  }

  private void createInitialRelease() throws Exception {
    Response response = put(client, RELEASES_ENDPOINT, resourceToString(INITIAL_RELEASE_RESOURCE));
    assertEquals(OK.getStatusCode(), response.getStatus());

    Release release = asRelease(response);
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
    Response response = get(client, RELEASES_ENDPOINT + "/" + releaseName);
    assertEquals(OK.getStatusCode(), response.getStatus());

    ReleaseView releaseView = asReleaseView(response);
    assertNotNull(releaseView);

    assertEquals(dictionaryVersion, releaseView.getDictionaryVersion());
    assertEquals(expectedReleaseState, releaseView.getState());
    assertEquals(ImmutableList.<String> of(), releaseView.getQueue());
    assertEquals(expectedSubmissionStates.size(), releaseView.getSubmissions().size());

    int i = 0;
    for (DetailedSubmission submission : releaseView.getSubmissions()) {
      assertEquals(submission.getProjectKey(), expectedSubmissionStates.get(i++), submission.getState());
    }
  }

  private void addProjects() throws IOException {
    status("admin", "Adding project 1...");
    Response response1 = post(client, PROJECTS_ENDPOINT, PROJECT1);
    assertEquals(CREATED.getStatusCode(), response1.getStatus());

    status("admin", "Adding project 2...");
    Response response2 = post(client, PROJECTS_ENDPOINT, PROJECT2);
    assertEquals(CREATED.getStatusCode(), response2.getStatus());

    status("admin", "Adding project 3...");
    Response response3 = post(client, PROJECTS_ENDPOINT, PROJECT3);
    assertEquals(CREATED.getStatusCode(), response3.getStatus());
  }

  private void enqueueProjects(String projectsToEnqueue, Status expectedStatus) throws Exception {
    status("user", "Getting queued projects...");
    Response response = get(client, QUEUE_ENDPOINT);
    String queued = asString(response);
    status("user", "Received queued projects: {}", queued);

    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals("[]", queued);

    status("user", "Enqueuing projects...");
    response = post(client, QUEUE_ENDPOINT, projectsToEnqueue);
    assertEquals(expectedStatus.getStatusCode(), response.getStatus());
    if (expectedStatus != NO_CONTENT) {
      assertEquals("{\"code\":\"" + INVALID_STATE.getFrontEndString() + "\",\"parameters\":[\"" + VALID + "\"]}",
          asString(response));
    }
  }

  private void checkValidations() throws Exception {
    status("user", "Getting release 1...");
    Response response = get(client, INITIAL_RELEASE_ENDPOINT);
    assertEquals(OK.getStatusCode(), response.getStatus());

    status("user", "Checking validated submission 1...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT1_NAME, VALID);

    status("user", "Checking validated submission 2...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT2_NAME, INVALID);

    status("user", "Checking validated submission 2...");
    checkValidatedSubmission(INITITAL_RELEASE_NAME, PROJECT3_NAME, INVALID);

    // check no errors for project 1
    assertEmptyFile(DCC_ROOT_DIR, PROJECT1_VALIDATION_DIR + "/donor.internal" + SEPARATOR + "errors.json");
    assertEmptyFile(DCC_ROOT_DIR, PROJECT1_VALIDATION_DIR + "/specimen.internal" + SEPARATOR + "errors.json");
    assertEmptyFile(DCC_ROOT_DIR, PROJECT1_VALIDATION_DIR + "/specimen.external" + SEPARATOR + "errors.json");
    // TODO add more
  }

  private void addOffendingCodeList() throws IOException {
    // Ensure codelist is present
    Response response = get(client, CODELISTS_ENDPOINT);
    assertEquals(OK.getStatusCode(), response.getStatus());
    val codeListName = "appendix_B10";
    assertTrue(asString(response).contains("\"" + codeListName + "\""));

    // Attempt to add it again
    response = post(client, CODELISTS_ENDPOINT, "[{\"name\": \"someName\"}, {\"name\": \"" + codeListName + "\"}]");
    assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
  }

  private void addValidCodeLists() throws IOException {
    Response response = post(client, CODELISTS_ENDPOINT, "[{\"name\": \"someName\"}, {\"name\": \"someNewName\"}]");
    assertEquals(CREATED.getStatusCode(), response.getStatus());
  }

  private void addCodeListTerm() throws Exception {
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED, asList(VALID, INVALID, INVALID));

    // TODO: Get codelist dynamically
    Response response = post(client, CODELISTS_ENDPOINT + "/GLOBAL.0.platform.v1/terms",
        "[{\"code\": \"81\", \"value\": \"new value 1\"}, {\"code\": \"82\", \"value\": \"new value 2\"}]");
    assertEquals(CREATED.getStatusCode(), response.getStatus());

    // Only the INVALID ones should have been reset (DCC-851)
    checkRelease(INITITAL_RELEASE_NAME, FIRST_DICTIONARY_VERSION, OPENED,
        asList(VALID, NOT_VALIDATED, NOT_VALIDATED));
  }

  private void checkValidatedSubmission(String release, String project, SubmissionState expectedSubmissionState)
      throws Exception {
    DetailedSubmission detailedSubmission;
    do {
      sleepUninterruptibly(2, SECONDS);

      status("user", "Polling submission status...");
      Response response = get(client, INITIAL_RELEASE_SUBMISSIONS_ENDPOINT + "/" + project);
      detailedSubmission = asDetailedSubmission(response);
      status("user", "Received submission status: {}", detailedSubmission);

      assertEquals(OK.getStatusCode(), response.getStatus());
    } while (detailedSubmission.getState() == QUEUED || detailedSubmission.getState() == VALIDATING);

    assertEquals(project, expectedSubmissionState, detailedSubmission.getState());
  }

  private void releaseInitialRelease() {
    // Attempts releasing (expect failure)
    status("admin", "Releasing attempt 1 (should fail)...");
    Response response = post(client, NEXT_RELEASE_ENPOINT, NEXT_RELEASE);
    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus()); // no signed off projects

    // Sign off
    status("user", "Signing off project {}", PROJECT_TO_SIGN_OFF);
    response = post(client, SIGNOFF_ENDPOINT, PROJECT_TO_SIGN_OFF);
    assertEquals(OK.getStatusCode(), response.getStatus());

    // Attempt releasing again
    status("admin", "Releasing attempt 2 (should pass)...");
    response = post(client, NEXT_RELEASE_ENPOINT, NEXT_RELEASE);
    assertEquals(asString(response), OK.getStatusCode(), response.getStatus());

    // Attempt releasing one too many times
    status("admin", "Releasing attempt 3 (should fail)...");
    response = post(client, NEXT_RELEASE_ENPOINT, NEXT_RELEASE);
    assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  private void updateDictionary(String dictionary, String dictionaryVersion, int expectedStatus)
      throws Exception {
    String updatedSecondDictionary = dictionary.replace("Unique identifier for the donor",
        "Unique identifier for the donor (update" + ++dictionaryUpdateCount + ")");
    assertTrue(dictionary, dictionary.equals(updatedSecondDictionary) == false);

    status("admin", "Updating dicionary...");
    Response response = put(client, DICTIONARIES_ENDPOINT + "/" + dictionaryVersion, updatedSecondDictionary);
    assertEquals(response.getHeaders().toString(), expectedStatus, response.getStatus());
  }

  private void updateRelease(String updatedReleaseRelPath) throws Exception {
    Response response = put(client, UPDATE_RELEASE_ENDPOINT, resourceToString(updatedReleaseRelPath));
    assertEquals(OK.getStatusCode(), response.getStatus());

    Release release = asRelease(response);
    assertEquals(NEXT_RELEASE_NAME, release.getName());
  }

  private static void status(String phase, String message, Object... args) {
    log.info(repeat("-", 100));
    log.info("[" + phase + "] " + message, args);
    log.info(repeat("-", 100));
  }

}
