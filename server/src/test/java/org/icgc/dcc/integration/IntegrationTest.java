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

import javax.ws.rs.MessageProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.jersey.internal.util.Base64;
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
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.code.morphia.Datastore;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.typesafe.config.ConfigFactory;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ConfigModule.class, CoreModule.class,//
HttpModule.class, JerseyModule.class,// TODO: find out why those two seem necessary
MorphiaModule.class, FileSystemModule.class })
public class IntegrationTest {

  @Inject
  private Datastore datastore;

  private static final String DCC_ROOT_DIR = ConfigFactory.load().getString("fs.root");

  static private Thread server;

  private final Client client = ClientFactory.newClient();

  private static final String BASEURI = "http://localhost:5380/ws";

  private static final String AUTHORIZATION = "X-DCC-Auth " + Base64.encodeAsString("admin:adminspasswd");

  private WebTarget target;

  @Before
  public void startServer() throws InterruptedException, IOException {
    clearFS();
    clearDB();

    server = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          Main.main(null);
        } catch(IOException e) {
          System.err.println("Problem starting server");
          e.printStackTrace();
        }
      }
    });
    server.start();
    Thread.sleep(5000);
  }

  private void clearDB() throws IOException, InterruptedException {
    datastore.getDB().dropDatabase();
    Thread.sleep(1000);
  }

  private void clearFS() throws IOException {
    FileUtils.deleteDirectory(new File(DCC_ROOT_DIR));
  }

  @After
  public void stopServer() {
    server.interrupt();
  }

  @Test
  public void test_IntegrationTest() throws JsonParseException, JsonMappingException, MessageProcessingException,
      IllegalStateException, IOException, InterruptedException {
    test_feedDB();

    test_createInitialRelease("/integrationtest/initRelease.json");

    test_checkRelease("release1", "0.6c", ReleaseState.OPENED, Arrays.<SubmissionState> asList());

    test_addProjects();

    test_checkRelease("release1", "0.6c", ReleaseState.OPENED, Arrays.<SubmissionState> asList(
        SubmissionState.NOT_VALIDATED, SubmissionState.NOT_VALIDATED, SubmissionState.NOT_VALIDATED));

    test_feedFileSystem();

    test_checkQueueIsEmpty();

    test_queueProjects();

    test_checkSubmissionsStates();

    test_fileIsEmpty(DCC_ROOT_DIR, "release1/project1/.validation/donor.internal#errors.json");
    test_fileIsEmpty(DCC_ROOT_DIR, "release1/project1/.validation/specimen.internal#errors.json");
    test_fileIsEmpty(DCC_ROOT_DIR, "release1/project1/.validation/specimen.external#errors.json");

    test_releaseFirstRelease();

    test_checkRelease("release1", "0.6c", ReleaseState.COMPLETED,
        Arrays.<SubmissionState> asList(SubmissionState.SIGNED_OFF));

    test_checkRelease("release2", "0.6c", ReleaseState.OPENED, Arrays.<SubmissionState> asList(
        SubmissionState.NOT_VALIDATED, SubmissionState.INVALID, SubmissionState.INVALID));

    test_updateRelease("/integrationtest/updatedRelease.json");

    test_checkRelease("release2", "0.6d", ReleaseState.OPENED, Arrays.<SubmissionState> asList(
        SubmissionState.NOT_VALIDATED, SubmissionState.NOT_VALIDATED, SubmissionState.NOT_VALIDATED));
  }

  private void test_addProjects() throws IOException {
    Response response1 =
        sendPostRequest("/projects",
            "{\"name\":\"Project One\",\"key\":\"project1\",\"users\":[\"admin\"],\"groups\":[\"admin\"]}");
    assertEquals(201, response1.getStatus());
    Response response2 =
        sendPostRequest("/projects",
            "{\"name\":\"Project Two\",\"key\":\"project2\",\"users\":[\"admin\", \"brett\"],\"groups\":[\"admin\"]}");
    assertEquals(201, response2.getStatus());
    Response response3 =
        sendPostRequest("/projects",
            "{\"name\":\"Project Three\",\"key\":\"project3\",\"users\":[\"admin\"],\"groups\":[\"admin\"]}");
    assertEquals(201, response3.getStatus());
  }

  private void test_feedFileSystem() throws IOException {
    // TODO ideally we should use a sftp client to upload data files
    File srcDir = new File("src/test/resources/integrationtest/fs/");
    File destDir = new File(DCC_ROOT_DIR);
    FileUtils.copyDirectory(srcDir, destDir);

    srcDir = new File("src/test/resources/integrationtest/fs/SystemFiles");
    destDir = new File(DCC_ROOT_DIR + "/release1/SystemFiles");
    FileUtils.copyDirectory(srcDir, destDir);
  }

  private void test_feedDB() throws InvocationException, NullPointerException, IllegalArgumentException, IOException {
    this.client.target(BASEURI).path("/seed/dictionaries").request(MediaType.APPLICATION_JSON)
        .header("Authorization", AUTHORIZATION)
        .post(Entity.entity("[" + this.resourceToString("/dictionary.json") + "]", MediaType.APPLICATION_JSON));
    String secondDictionaryPath = "/integrationtest/secondDictionary.json"; // careful, also updated by converter, do
                                                                            // not edit manually, update via
                                                                            // DictionaryConverterTest.updateSecondDictionaryContent()
                                                                            // instead
    this.client.target(BASEURI).path("/seed/dictionaries").request(MediaType.APPLICATION_JSON)
        .header("Authorization", AUTHORIZATION)
        .post(Entity.entity("[" + this.resourceToString(secondDictionaryPath) + "]", MediaType.APPLICATION_JSON));
    this.client.target(BASEURI).path("/seed/codelists").request(MediaType.APPLICATION_JSON)
        .header("Authorization", AUTHORIZATION)
        .post(Entity.entity(this.resourceToString("/integrationtest/codelists.json"), MediaType.APPLICATION_JSON));
  }

  private void test_checkSubmissionsStates() throws IOException, InterruptedException {
    Response response = sendGetRequest("/releases/release1");
    assertEquals(200, response.getStatus());

    Submission submission;
    do {
      response = sendGetRequest("/releases/release1/submissions/project1");
      assertEquals(200, response.getStatus());
      submission = new ObjectMapper().readValue(response.readEntity(String.class), DetailedSubmission.class);
      Thread.sleep(2000);
    } while(submission.getState() == SubmissionState.QUEUED);
    assertEquals(SubmissionState.VALID, submission.getState());

    do {
      response = sendGetRequest("/releases/release1/submissions/project2");
      assertEquals(200, response.getStatus());
      submission = new ObjectMapper().readValue(response.readEntity(String.class), DetailedSubmission.class);
      Thread.sleep(2000);
    } while(submission.getState() == SubmissionState.QUEUED);
    assertEquals(SubmissionState.INVALID, submission.getState());

    do {
      response = sendGetRequest("/releases/release1/submissions/project3");
      assertEquals(200, response.getStatus());
      submission = new ObjectMapper().readValue(response.readEntity(String.class), DetailedSubmission.class);
      Thread.sleep(2000);
    } while(submission.getState() == SubmissionState.QUEUED);
    assertEquals(SubmissionState.INVALID, submission.getState());
  }

  private void test_fileIsEmpty(String dir, String path) throws IOException {
    File errorFile = new File(dir, path);
    assertTrue("Expected file does not exist: " + path, errorFile.exists());
    assertTrue("Expected empty file: " + path, FileUtils.readFileToString(errorFile).isEmpty());
  }

  private void test_releaseFirstRelease() throws IOException {
    // Expect 400 Bad Request because no projects are signed off
    Response response = sendPostRequest("/nextRelease", "{\"name\": \"release2\"}");
    assertEquals(400, response.getStatus());

    // Sign off on a project
    response = sendPostRequest("/nextRelease/signed", "[\"project1\"]");
    assertEquals(200, response.getStatus());

    // Release again, expect 200 OK
    response = sendPostRequest("/nextRelease", "{\"name\": \"release2\"}");
    assertEquals(200, response.getStatus());

    // Release again, expect 400 Bad Request because of the duplicate release
    response = sendPostRequest("/nextRelease", "{\"name\": \"release2\"}");
    assertEquals(400, response.getStatus());
  }

  private void test_checkRelease(String releaseName, String dictionaryVersion, ReleaseState state,
      List<SubmissionState> states) throws IOException, JsonParseException, JsonMappingException {
    Response response = sendGetRequest("/releases/" + releaseName);
    assertEquals(200, response.getStatus());

    ReleaseView release = new ObjectMapper().readValue(response.readEntity(String.class), ReleaseView.class);
    assertNotNull(release);
    assertEquals(dictionaryVersion, release.getDictionaryVersion());
    assertEquals(ImmutableList.<String> of(), release.getQueue());
    assertEquals(state, release.getState());
    assertEquals(states.size(), release.getSubmissions().size());
    int i = 0;
    for(DetailedSubmission submission : release.getSubmissions()) {
      assertEquals(states.get(i++), submission.getState());
    }
  }

  private void test_checkQueueIsEmpty() throws IOException {
    Response response = sendGetRequest("/nextRelease/queue");
    assertEquals(200, response.getStatus());
    assertEquals("[]", response.readEntity(String.class));
  }

  private void test_createInitialRelease(String initReleaseRelPath) throws IOException, JsonParseException,
      JsonMappingException {
    Response response = sendPutRequest("/releases", resourceToString(initReleaseRelPath));
    assertEquals(200, response.getStatus());
    Release release = new ObjectMapper().readValue(response.readEntity(String.class), Release.class);
    assertEquals("release1", release.getName());
  }

  private String resourceToString(String resourcePath) throws IOException {
    return Resources.toString(this.getClass().getResource(resourcePath), Charsets.UTF_8);
  }

  private void test_queueProjects() throws IOException, JsonParseException, JsonMappingException {
    Response response =
        sendPostRequest(
            "/nextRelease/queue",
            "[{\"key\": \"project1\", \"emails\": [\"a@a.ca\"]}, {\"key\": \"project2\", \"emails\": [\"a@a.ca\"]}, {\"key\": \"project3\", \"emails\": [\"a@a.ca\"]}]");
    assertEquals(200, response.getStatus());
  }

  private void test_updateRelease(String updatedReleaseRelPath) throws IOException, JsonParseException,
      JsonMappingException {
    Response response = sendPutRequest("/nextRelease/update", resourceToString(updatedReleaseRelPath));
    assertEquals(200, response.getStatus());
    Release release = new ObjectMapper().readValue(response.readEntity(String.class), Release.class);
    assertEquals("release2", release.getName());
  }

  private Response sendPutRequest(String requestPath, String payload) throws IOException {

    this.target = this.client.target(BASEURI).path(requestPath);
    Response response =
        this.target.request(MediaType.APPLICATION_JSON).header("Authorization", AUTHORIZATION)
            .put(Entity.entity(payload, MediaType.APPLICATION_JSON));
    return response;
  }

  private Response sendGetRequest(String requestPath) throws IOException {
    this.target = this.client.target(BASEURI).path(requestPath);
    Response response = this.target.request(MediaType.APPLICATION_JSON).header("Authorization", AUTHORIZATION).get();
    return response;
  }

  private Response sendPostRequest(String requestPath, String payload) throws IOException {
    this.target = this.client.target(BASEURI).path(requestPath);
    Response response =
        this.target.request(MediaType.APPLICATION_JSON).header("Authorization", AUTHORIZATION)
            .post(Entity.entity(payload, MediaType.APPLICATION_JSON));
    return response;
  }
}
