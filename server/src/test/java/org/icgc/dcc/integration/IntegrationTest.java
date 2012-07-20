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

import java.io.IOException;

import javax.ws.rs.MessageProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.jersey.internal.util.Base64;
import org.icgc.dcc.Main;
import org.icgc.dcc.release.model.Release;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 
 */
public class IntegrationTest {
  static private Thread server;

  private Client client;

  private final String baseURI = "http://localhost:5380/ws/releases";

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

  @AfterClass
  static public void stopServer() {
    server.interrupt();
  }

  @Test
  public void test_release_resource() throws JsonParseException, JsonMappingException, MessageProcessingException,
      IllegalStateException, IOException {

    // create client for the server
    this.client = ClientFactory.newClient();
    this.target = this.client.target(baseURI);

    // get release1
    this.target = this.client.target(baseURI).path("/{name}");
    Response response =
        this.target.pathParam("name", "release1").request(MediaType.APPLICATION_JSON)
            .header("Authorization", "X-DCC-Auth " + Base64.encodeAsString("admin:adminspasswd")).get();
    Release release = new ObjectMapper().readValue(response.readEntity(String.class), Release.class);
    assertEquals("release1", release.getName());
  }

}
