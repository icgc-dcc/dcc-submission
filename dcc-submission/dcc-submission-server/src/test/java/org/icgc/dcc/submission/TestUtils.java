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
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Resources.getResource;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.glassfish.grizzly.http.util.Header.Authorization;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.Response;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.codehaus.jackson.map.ObjectMapper;
import org.glassfish.jersey.internal.util.Base64;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseView;

import com.google.common.io.Resources;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Utility class for integration test (to help un-clutter it).
 */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class TestUtils {

  /**
   * Jackson constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();

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
  public static final String VALIDATION_ENDPOINT = NEXT_RELEASE_ENPOINT + "/validation";

  /**
   * URL constants.
   */
  public static final String BASEURI = "http://localhost:5380/ws";
  public static final String AUTHORIZATION_HEADER_VALUE = "X-DCC-Auth " + Base64.encodeAsString("admin:adminspasswd");

  /**
   * Test configuration constants.
   */
  public static final File TEST_CONFIG_FILE = new File("src/test/conf/application.conf");
  public static final Config TEST_CONFIG = ConfigFactory.parseFile(TEST_CONFIG_FILE).resolve();

  @SneakyThrows
  public static String resourceToString(String resourcePath) {
    return Resources.toString(TestUtils.class.getResource(resourcePath), UTF_8);
  }

  @SneakyThrows
  public static String resourceToString(URL resourceUrl) {
    return Resources.toString(resourceUrl, UTF_8);
  }

  @SneakyThrows
  public static String resourceToJsonArray(String resourcePath) {
    return "[" + resourceToString(resourcePath) + "]";
  }

  public static CodeList getChromosomeCodeList() {
    val targetCodeListName = "GLOBAL.0.chromosome.v1";
    for (val codeList : codeLists()) {
      if (targetCodeListName.equals(codeList.getName())) {
        return codeList;
      }
    }

    throw new IllegalStateException("Code list '" + targetCodeListName + "' is not available");
  }

  @SneakyThrows
  public static List<CodeList> codeLists() {
    Iterator<CodeList> codeLists = MAPPER.reader(CodeList.class).readValues(getDccResource("CodeList.json"));
    return newArrayList(codeLists);
  }

  @SneakyThrows
  public static String codeListsToString() {
    return MAPPER.writeValueAsString(codeLists());
  }

  @SneakyThrows
  public static Dictionary dictionary() {
    return MAPPER.reader(Dictionary.class).readValue(getDccResource("Dictionary.json"));
  }

  @SneakyThrows
  public static String dictionaryToString() {
    return MAPPER.writeValueAsString(dictionary());
  }

  @SneakyThrows
  public static String dictionaryVersion(String dictionaryJson) {
    val dictionaryNode = MAPPER.readTree(dictionaryJson);

    return dictionaryNode.get("version").asText();
  }

  public static Builder build(Client client, String path) {
    return client
        .target(BASEURI)
        .path(path)
        .request(APPLICATION_JSON)
        .header(Authorization.toString(), AUTHORIZATION_HEADER_VALUE);
  }

  @SneakyThrows
  public static Response get(Client client, String endPoint) {
    log.info("GET {}", endPoint);
    return build(client, endPoint).get();
  }

  public static Response post(Client client, String endPoint, String payload) {
    log.info("POST {} {}", endPoint, abbreviate(payload, 1000));
    return build(client, endPoint).post(Entity.entity(payload, APPLICATION_JSON));
  }

  public static Response put(Client client, String endPoint, String payload) {
    log.info("PUT {} {}", endPoint, abbreviate(payload, 1000));
    return build(client, endPoint).put(Entity.entity(payload, APPLICATION_JSON));
  }

  public static Response delete(Client client, String endPoint) {
    log.info("DELETE {}", endPoint);
    return build(client, endPoint).delete();
  }

  public static String asString(Response response) {
    return response.readEntity(String.class);
  }

  @SneakyThrows
  public static Release asRelease(Response response) {
    return MAPPER.readValue(asString(response), Release.class);
  }

  @SneakyThrows
  public static ReleaseView asReleaseView(Response response) {
    return MAPPER.readValue(asString(response), ReleaseView.class);
  }

  @SneakyThrows
  public static DetailedSubmission asDetailedSubmission(Response response) {
    return MAPPER.readValue(asString(response), DetailedSubmission.class);
  }

  private static URL getDccResource(String resourceName) {
    return getResource("org/icgc/dcc/resources/" + resourceName);
  }

}
