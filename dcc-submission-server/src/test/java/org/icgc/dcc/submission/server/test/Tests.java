/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.server.test;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Strings.repeat;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.icgc.dcc.submission.dictionary.util.Dictionaries.readResourcesDictionary;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import org.icgc.dcc.common.core.model.DataType.DataTypes;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.dictionary.model.RestrictionType;
import org.icgc.dcc.submission.dictionary.util.Dictionaries;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseView;
import org.icgc.dcc.submission.validation.primary.restriction.ScriptRestriction;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import com.mongodb.BasicDBObject;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for integration test (to help declutter it).
 */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class Tests {

  /**
   * Jackson constants.
   */
  public static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
      .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

  /**
   * Endpoint path constants.
   */
  public static final String SEED_ENDPOINT = "/ws/seed";
  public static final String SEED_CODELIST_ENDPOINT = SEED_ENDPOINT + "/codelists";
  public static final String SEED_DICTIONARIES_ENDPOINT = SEED_ENDPOINT + "/dictionaries";
  public static final String DICTIONARIES_ENDPOINT = "/ws/dictionaries";
  public static final String CODELISTS_ENDPOINT = "/ws/codeLists";
  public static final String PROJECTS_ENDPOINT = "/ws/projects";
  public static final String RELEASES_ENDPOINT = "/ws/releases";
  public static final String NEXT_RELEASE_ENPOINT = "/ws/nextRelease";
  public static final String UPDATE_RELEASE_ENDPOINT = NEXT_RELEASE_ENPOINT + "/update";
  public static final String SIGNOFF_ENDPOINT = NEXT_RELEASE_ENPOINT + "/signed";
  public static final String QUEUE_ENDPOINT = NEXT_RELEASE_ENPOINT + "/queue";
  public static final String VALIDATION_ENDPOINT = NEXT_RELEASE_ENPOINT + "/validation";

  @SneakyThrows
  private static SubmissionProperties readConfig(File configFile) {
    return new ObjectMapper(new YAMLFactory()).readValue(configFile, SubmissionProperties.class);
  }

  @SneakyThrows
  public static String resourceToString(String resourcePath) {
    return Resources.toString(Tests.class.getResource(resourcePath), UTF_8);
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
  public static Dictionary dictionary() {
    return readResourcesDictionary("0.11c");
  }

  public static List<CodeList> codeLists() {
    return Dictionaries.readResourcesCodeLists();
  }

  public static List<String> getFieldNames(FileType type) {
    return dictionary().getFileSchema(type).getFieldNames();
  }

  @SneakyThrows
  public static String codeListsToString() {
    return MAPPER.writeValueAsString(codeLists());
  }

  @SneakyThrows
  public static Dictionary addScript(Dictionary dictionary, String fileSchemaName, String fieldName, String script,
      String description) {
    for (val fileSchema : dictionary.getFiles()) {
      if (fileSchema.getName().equals(fileSchemaName)) {
        for (val field : fileSchema.getFields()) {
          if (field.getName().equals(fieldName)) {
            val config = new BasicDBObject();
            config.put(ScriptRestriction.PARAM, script);
            config.put(ScriptRestriction.PARAM_DESCRIPTION, description);

            val restriction = new Restriction();
            restriction.setType(RestrictionType.SCRIPT);
            restriction.setConfig(config);

            val restrictions = field.getRestrictions();
            restrictions.add(restriction);
          }
        }
      }
    }

    return dictionary;
  }

  @SneakyThrows
  public static String dataTypesToString() {
    return MAPPER.writeValueAsString(DataTypes.values());
  }

  public static String dictionaryToString() {
    return dictionaryToString(dictionary());
  }

  @SneakyThrows
  public static String dictionaryToString(Dictionary dictionary) {
    return MAPPER.writeValueAsString(dictionary);
  }

  @SneakyThrows
  public static String dictionaryVersion(String dictionaryJson) {
    val dictionaryNode = MAPPER.readTree(dictionaryJson);

    return dictionaryNode.get("version").asText();
  }

  public static String replaceDictionaryVersion(String dictionary, String oldVersion, String newVersion) {
    return dictionary
        .replaceAll(
            "\"version\": *\"" + Pattern.quote(oldVersion) + "\"",
            "\"version\": \"" + newVersion + "\"");
  }

  @SneakyThrows
  public static ResponseEntity<String> get(TestRestTemplate restTemplate, String endPoint) {
    banner();
    log.info("GET {}", endPoint);
    banner();
    return logPotentialErrors(restTemplate.getForEntity(endPoint, String.class));
  }

  /**
   * TODO: ensure payload is valid json and fail fast (also for other VERBS)
   */
  public static ResponseEntity<String> post(TestRestTemplate restTemplate, String endPoint, String payload) {
    payload = normalize(payload);

    banner();
    log.info("POST {} {}", endPoint, abbreviate(payload, 1000));
    banner();

    val headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON_UTF8);

    val entity = new HttpEntity<String>(payload, headers);

    return logPotentialErrors(
        restTemplate.exchange(endPoint, HttpMethod.POST, entity, String.class));
  }

  public static ResponseEntity<String> put(TestRestTemplate restTemplate, String endPoint, String payload) {
    payload = normalize(payload);

    banner();
    log.info("PUT {} {}", endPoint, abbreviate(payload, 1000));
    banner();

    val headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON_UTF8);

    val entity = new HttpEntity<String>(payload, headers);

    return logPotentialErrors(
        restTemplate.exchange(endPoint, HttpMethod.PUT, entity, String.class));
  }

  public static ResponseEntity<String> delete(TestRestTemplate restTemplate, String endPoint) {
    banner();
    log.info("DELETE {}", endPoint);
    banner();

    return logPotentialErrors(restTemplate.exchange(endPoint, HttpMethod.DELETE, null, String.class));
  }

  private static ResponseEntity<String> logPotentialErrors(ResponseEntity<String> response) {
    int status = response.getStatusCode().value();
    if (status < 200 || status >= 300) {
      log.warn("There was an erroneous reponse: '{}', '{}'", status, response.getBody());
    }
    return response;
  }

  public static String asString(ResponseEntity<String> response) {
    return response.getBody();
  }

  @SneakyThrows
  public static JsonNode $(String json) {
    return MAPPER.readTree(json);
  }

  @SneakyThrows
  public static JsonNode $(ResponseEntity<String> response) {
    return $(asString(response));
  }

  @SneakyThrows
  public static Release asRelease(ResponseEntity<String> response) {
    return MAPPER.readValue(asString(response), Release.class);
  }

  @SneakyThrows
  public static ReleaseView asReleaseView(ResponseEntity<String> response) {
    return MAPPER.readValue(asString(response), ReleaseView.class);
  }

  @SneakyThrows
  public static DetailedSubmission asDetailedSubmission(ResponseEntity<String> response) {
    return MAPPER.readValue(asString(response), DetailedSubmission.class);
  }

  @SneakyThrows
  private static String normalize(String json) {
    val node = MAPPER.readTree(json);
    val writer = MAPPER.writerWithDefaultPrettyPrinter();
    return writer.writeValueAsString(node);
  }

  private static void banner() {
    log.info("{}", repeat("\u00B7", 80));
  }

}
