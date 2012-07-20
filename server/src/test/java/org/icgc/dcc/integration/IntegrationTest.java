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

import java.io.File;
import java.io.IOException;

import javax.ws.rs.MessageProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.Entity;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * 
 */
public class IntegrationTest {
  static private Thread server;

  private Client client = ClientFactory.newClient();

  private final String baseURI = "http://localhost:5380/ws";

  private final String AUTHORIZATION = "X-DCC-Auth " + Base64.encodeAsString("admin:adminspasswd");

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
    this.client.target(baseURI).path("/seed/releases").queryParam("delete", "true").request(MediaType.APPLICATION_JSON)
        .header("Authorization", AUTHORIZATION).post(Entity.entity("[]", MediaType.APPLICATION_JSON));
    this.client.target(baseURI).path("/seed/projects").queryParam("delete", "true").request(MediaType.APPLICATION_JSON)
        .header("Authorization", AUTHORIZATION).post(Entity.entity("[]", MediaType.APPLICATION_JSON));
    this.client.target(baseURI).path("/seed/dictionaries").queryParam("delete", "true")
        .request(MediaType.APPLICATION_JSON).header("Authorization", AUTHORIZATION)
        .post(Entity.entity("[]", MediaType.APPLICATION_JSON));
    this.client.target(baseURI).path("/seed/codelists").queryParam("delete", "true")
        .request(MediaType.APPLICATION_JSON).header("Authorization", AUTHORIZATION)
        .post(Entity.entity("[]", MediaType.APPLICATION_JSON));
    Thread.sleep(1000);
  }

  public void clearFS() throws IOException {
    FileUtils.deleteDirectory(new File("/tmp/dcc_root_dir/"));
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

    test_createInitialRelease();

    test_feedFileSystem();

    test_checkQueueIsEmpty();

    test_queueProjects();

    test_checkSubmissionsStates();
  }

  private void test_feedFileSystem() throws IOException {
    // TODO ideally we should use a sftp client to upload data files
    File srcDir = new File("src/test/resources/integrationtest/fs/");
    File destDir = new File("/tmp/dcc_root_dir/");
    FileUtils.copyDirectory(srcDir, destDir);
  }

  private void test_checkSubmissionsStates() throws IOException {
    // TODO check actual submissions state in response

    Response response = sendGetRequest("/releases/release1");
    assertEquals(200, response.getStatus());

    response = sendGetRequest("/releases/release1/submissions/project1");
    assertEquals(200, response.getStatus());

    response = sendGetRequest("/releases/release1/submissions/project2");
    assertEquals(200, response.getStatus());

    response = sendGetRequest("/releases/release1/submissions/project2");
    assertEquals(200, response.getStatus());
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

    this.target = this.client.target(baseURI).path(requestPath);
    Response response =
        this.target.request(MediaType.APPLICATION_JSON).header("Authorization", AUTHORIZATION)
            .put(Entity.entity(payload, MediaType.APPLICATION_JSON));
    return response;
  }

  private Response sendGetRequest(String requestPath) throws IOException {
    this.target = this.client.target(baseURI).path(requestPath);
    Response response = this.target.request(MediaType.APPLICATION_JSON).header("Authorization", AUTHORIZATION).get();
    return response;
  }

  private Response sendPostRequest(String requestPath, String payload) throws IOException {
    this.target = this.client.target(baseURI).path(requestPath);
    Response response =
        this.target.request(MediaType.APPLICATION_JSON).header("Authorization", AUTHORIZATION)
            .post(Entity.entity(payload, MediaType.APPLICATION_JSON));
    return response;
  }

  private void originalTest() {
    // create client for the server
    this.client = ClientFactory.newClient();

    // get release1
    this.target = this.client.target(baseURI).path("/{name}");
    Response response =
        this.target.pathParam("name", "release1").request(MediaType.APPLICATION_JSON)
            .header("Authorization", "X-DCC-Auth " + Base64.encodeAsString("admin:adminspasswd")).get();
    Release release;
    try {
      release = new ObjectMapper().readValue(response.readEntity(String.class), Release.class);
      assertEquals("release1", release.getName());
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

}
