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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

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
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

public class IntegrationTest {
  /**
   * 
   */
  private static final String DCC_ROOT_DIR = "/tmp/dcc_root_dir/";

  static private Thread server;

  private final Client client = ClientFactory.newClient();

  private static final String BASEURI = "http://localhost:5380/ws";

  private static final String AUTHORIZATION = "X-DCC-Auth " + Base64.encodeAsString("admin:adminspasswd");

  private WebTarget target;

  @BeforeClass
  static public void startServer() throws InterruptedException {
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

  public void clearDB() throws IOException, InterruptedException {
    this.client.target(BASEURI).path("/seed/releases").queryParam("delete", "true").request(MediaType.APPLICATION_JSON)
        .header("Authorization", AUTHORIZATION).post(Entity.entity("[]", MediaType.APPLICATION_JSON));
    this.client.target(BASEURI).path("/seed/projects").queryParam("delete", "true").request(MediaType.APPLICATION_JSON)
        .header("Authorization", AUTHORIZATION).post(Entity.entity("[]", MediaType.APPLICATION_JSON));
    this.client.target(BASEURI).path("/seed/dictionaries").queryParam("delete", "true")
        .request(MediaType.APPLICATION_JSON).header("Authorization", AUTHORIZATION)
        .post(Entity.entity("[]", MediaType.APPLICATION_JSON));
    this.client.target(BASEURI).path("/seed/codelists").queryParam("delete", "true")
        .request(MediaType.APPLICATION_JSON).header("Authorization", AUTHORIZATION)
        .post(Entity.entity("[]", MediaType.APPLICATION_JSON));
    Thread.sleep(1000);
  }

  public void clearFS() throws IOException {
    FileUtils.deleteDirectory(new File(DCC_ROOT_DIR));
  }

  @AfterClass
  static public void stopServer() {
    server.interrupt();
  }

  @Test
  public void test_IntegrationTest() throws JsonParseException, JsonMappingException, MessageProcessingException,
      IllegalStateException, IOException, InterruptedException {

    clearDB();

    clearFS();

    test_feedDB();

    test_createInitialRelease();

    test_feedFileSystem();

    test_checkQueueIsEmpty();

    test_queueProjects();

    test_checkSubmissionsStates();

    test_fileIsEmpty(DCC_ROOT_DIR + "release1/project1/.validation/donor.internal#errors.json");
    test_fileIsEmpty(DCC_ROOT_DIR + "release1/project1/.validation/specimen.internal#errors.json");
    test_fileIsEmpty(DCC_ROOT_DIR + "release1/project1/.validation/specimen.external#errors.json");

    test_checkReleaseState("release1", ReleaseState.OPENED);

    test_releaseFirstRelease();

    test_checkReleaseState("release1", ReleaseState.COMPLETED);

    test_checkReleaseState("release2", ReleaseState.OPENED);
  }

  private void test_feedFileSystem() throws IOException {
    // TODO ideally we should use a sftp client to upload data files
    File srcDir = new File("src/test/resources/integrationtest/fs/");
    File destDir = new File(DCC_ROOT_DIR);
    FileUtils.copyDirectory(srcDir, destDir);
  }

  private void test_feedDB() throws InvocationException, NullPointerException, IllegalArgumentException, IOException {
    this.client.target(BASEURI).path("/seed/projects").request(MediaType.APPLICATION_JSON)
        .header("Authorization", AUTHORIZATION)
        .post(Entity.entity(this.resourceToString("/integrationtest/projects.json"), MediaType.APPLICATION_JSON));
    this.client.target(BASEURI).path("/seed/dictionaries").request(MediaType.APPLICATION_JSON)
        .header("Authorization", AUTHORIZATION)
        .post(Entity.entity("[" + this.resourceToString("/dictionary.json") + "]", MediaType.APPLICATION_JSON));
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
      submission = new ObjectMapper().readValue(response.readEntity(String.class), Submission.class);
      Thread.sleep(2000);
    } while(submission.getState() == SubmissionState.QUEUED);
    assertEquals(SubmissionState.VALID, submission.getState());

    response = sendGetRequest("/releases/release1/submissions/project2");
    assertEquals(200, response.getStatus());

    response = sendGetRequest("/releases/release1/submissions/project3");
    assertEquals(200, response.getStatus());
  }

  private void test_fileIsEmpty(String path) throws IOException {
    File errorFile = new File(path);
    assertTrue("Expected file does not exist: " + path, errorFile.exists());
    assertTrue("Expected empty file: " + path, FileUtils.readFileToString(errorFile).isEmpty());
  }

  private void test_releaseFirstRelease() throws IOException {
    // Expect 400 Bad Request because no projects are signed off
    Response response = sendPostRequest("/nextRelease", resourceToString("/integrationtest/nextRelease.json"));
    assertEquals(400, response.getStatus());

    // Sign off on a project
    response = sendPostRequest("/nextRelease/signed", "[\"project1\"]");
    assertEquals(200, response.getStatus());

    // Release again, expect 200 OK
    response = sendPostRequest("/nextRelease", resourceToString("/integrationtest/nextRelease.json"));
    assertEquals(200, response.getStatus());

    // Release again, expect 400 Bad Request because of the duplicate release
    response = sendPostRequest("/nextRelease", resourceToString("/integrationtest/nextRelease.json"));
    assertEquals(400, response.getStatus());
  }

  private void test_checkReleaseState(String releaseName, ReleaseState expectedState) throws IOException,
      InterruptedException {
    Response response = sendGetRequest("/releases/" + releaseName);
    assertEquals(200, response.getStatus());

    Release release = new ObjectMapper().readValue(response.readEntity(String.class), Release.class);
    assertEquals(expectedState, release.getState());
  }

  private void test_checkQueueIsEmpty() throws IOException {
    Response response = sendGetRequest("/nextRelease/queue");
    assertEquals(200, response.getStatus());
    assertEquals("[]", response.readEntity(String.class));
  }

  private void test_createInitialRelease() throws IOException, JsonParseException, JsonMappingException {
    Response response = sendPutRequest("/releases/release1", resourceToString("/integrationtest/initRelease.json"));
    assertEquals(200, response.getStatus());
    Release release = new ObjectMapper().readValue(response.readEntity(String.class), Release.class);
    assertEquals("release1", release.getName());
  }

  private String resourceToString(String resourcePath) throws IOException {
    return Resources.toString(this.getClass().getResource(resourcePath), Charsets.UTF_8);
  }

  private void test_queueProjects() throws IOException, JsonParseException, JsonMappingException {
    Response response = sendPostRequest("/nextRelease/queue", "[\"project1\", \"project2\", \"project3\"]");
    assertEquals(200, response.getStatus());
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
