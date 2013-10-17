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

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.getResource;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang.StringUtils.abbreviate;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.grizzly.http.util.Header;
import org.glassfish.jersey.internal.util.Base64;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseView;

import com.google.common.io.Resources;

/**
 * Utility class for integration test (to help un-clutter it).
 */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class TestUtils {

  /**
   * Endpoint path constants.
   */
  public static final String SEED_ENDPOINT = "/seed";
  public static final String SEED_CODELIST_ENDPOINT = SEED_ENDPOINT + "/codelists";
  public static final String SEED_DICTIONARIES_ENDPOINT = SEED_ENDPOINT + "/dictionaries";
  public static final String DICTIONARIES_ENDPOINT = "/dictionaries";
  public static final String CODELISTS_ENDPOINT = "/codeLists";
  public static final String PROJECTS_ENDPOINT = "/projects";
  public static final String RELEASES_ENDPOINT = "/releases";
  public static final String NEXT_RELEASE_ENPOINT = "/nextRelease";
  public static final String UPDATE_RELEASE_ENDPOINT = NEXT_RELEASE_ENPOINT + "/update";
  public static final String SIGNOFF_ENDPOINT = NEXT_RELEASE_ENPOINT + "/signed";
  public static final String QUEUE_ENDPOINT = NEXT_RELEASE_ENPOINT + "/queue";

  public static final String BASEURI = "http://localhost:5380/ws";
  public static final String AUTHORIZATION_HEADER_VALUE = "X-DCC-Auth "
      + Base64.encodeAsString("admin:adminspasswd");

  public static String resourceToString(String resourcePath) throws IOException {
    return Resources.toString(TestUtils.class.getResource(resourcePath), UTF_8);
  }

  public static String resourceToString(URL resourceUrl) throws IOException {
    return Resources.toString(resourceUrl, UTF_8);
  }

  @SneakyThrows
  public static String codeListsToString() {
    ObjectMapper mapper = new ObjectMapper();
    Iterator<CodeList> codeLists =
        mapper.reader(CodeList.class).readValues(getResource("org/icgc/dcc/resources/CodeList.json"));
    return mapper.writeValueAsString(codeLists);
  }

  @SneakyThrows
  public static String dictionaryToString() {
    ObjectMapper mapper = new ObjectMapper();
    Dictionary dictionary =
        mapper.reader(Dictionary.class).readValue(getResource("org/icgc/dcc/resources/Dictionary.json"));
    return mapper.writeValueAsString(dictionary);
  }

  public static String resourceToJsonArray(String resourcePath) throws IOException {
    return "[" + resourceToString(resourcePath) + "]";
  }

  public static Builder build(Client client, String endPoint) {
    return client.target(BASEURI).path(endPoint).request(MediaType.APPLICATION_JSON)
        .header(Header.Authorization.toString(), AUTHORIZATION_HEADER_VALUE);
  }

  public static Response get(Client client, String endPoint) throws IOException {
    log.info("GET {}", endPoint);
    return build(client, endPoint).get();
  }

  public static Response post(Client client, String endPoint, String payload) {
    log.info("POST {} {}", new Object[] { endPoint, abbreviate(payload, 1000) });
    return build(client, endPoint).post(Entity.entity(payload, MediaType.APPLICATION_JSON));
  }

  public static Response put(Client client, String endPoint, String payload) {
    log.info("PUT {} {}", new Object[] { endPoint, abbreviate(payload, 1000) });
    return build(client, endPoint).put(Entity.entity(payload, MediaType.APPLICATION_JSON));
  }

  public static String asString(Response response) {
    return response.readEntity(String.class);
  }

  public static Release asRelease(Response response) throws Exception {
    return new ObjectMapper().readValue(asString(response), Release.class);
  }

  public static ReleaseView asReleaseView(Response response) throws Exception {
    return new ObjectMapper().readValue(asString(response), ReleaseView.class);
  }

  public static DetailedSubmission asDetailedSubmission(Response response) throws Exception {
    return new ObjectMapper().readValue(asString(response), DetailedSubmission.class);
  }

}
