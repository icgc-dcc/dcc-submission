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

import java.io.IOException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.jersey.internal.util.Base64;
import org.icgc.dcc.release.model.DetailedSubmission;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

/**
 * Utils class for integration test (to help un-clutter it).
 */
public class TestUtils {

  private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

  private static final String AUTHORIZATION = "X-DCC-Auth " //
      + Base64.encodeAsString("admin:adminspasswd"); // only true for development realm

  private static final String BASEURI = "http://localhost:5380/ws";

  static String resourceToString(String resourcePath) throws IOException {
    return Resources.toString(TestUtils.class.getResource(resourcePath), Charsets.UTF_8);
  }

  static String resourceToJsonArray(String resourcePath) throws IOException {
    return "[" + resourceToString(resourcePath) + "]";
  }

  static Builder build(Client client, String endPoint) {
    return client.target(BASEURI).path(endPoint).request(MediaType.APPLICATION_JSON)
        .header(Header.Authorization.toString(), AUTHORIZATION);
  }

  static Response get(Client client, String endPoint) throws IOException {
    log.info("GET {}", endPoint);
    return build(client, endPoint).get();
  }

  static Response post(Client client, String endPoint, String payload) {
    log.info("POST {} {}", new Object[] { endPoint, payload });
    return build(client, endPoint).post(Entity.entity(payload, MediaType.APPLICATION_JSON));
  }

  static Response put(Client client, String endPoint, String payload) {
    log.info("PUT {} {}", new Object[] { endPoint, payload });
    return build(client, endPoint).put(Entity.entity(payload, MediaType.APPLICATION_JSON));
  }

  static String asString(Response response) {
    return response.readEntity(String.class);
  }

  static Release asRelease(Response response) throws Exception {
    return new ObjectMapper().readValue(asString(response), Release.class);
  }

  static ReleaseView asReleaseView(Response response) throws Exception {
    return new ObjectMapper().readValue(asString(response), ReleaseView.class);
  }

  static DetailedSubmission asDetailedSubmission(Response response) throws Exception {
    return new ObjectMapper().readValue(asString(response), DetailedSubmission.class);
  }
}
