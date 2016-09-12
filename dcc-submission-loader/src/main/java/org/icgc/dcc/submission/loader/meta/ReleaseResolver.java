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
package org.icgc.dcc.submission.loader.meta;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.icgc.dcc.common.core.util.Separators.COLON;

import java.util.List;
import java.util.function.Predicate;

import javax.ws.rs.core.MediaType;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.loader.model.Project;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;

@Slf4j
public class ReleaseResolver {

  private final String url;
  private final String userName;
  private final String password;

  public ReleaseResolver(@NonNull String url, @NonNull String user, @NonNull String password) {
    this.url = url + "/ws/releases";
    this.userName = user;
    this.password = password;
  }

  public Iterable<String> getReleases() {
    val releases = ImmutableList.<String> builder();
    for (val element : getResponse(url)) {
      releases.add(element.get("name").textValue());
    }

    return releases.build();
  }

  public String getDictionaryVersion(@NonNull String release) {
    release = release.toUpperCase();
    for (val element : getResponse(url)) {
      val releaseName = element.get("name").textValue();
      if (release.equals(releaseName)) {
        return element.get("dictionaryVersion").textValue();
      }
    }

    throw new IllegalArgumentException(format("Failed to resolve dictionary version for release '%s'", release));
  }

  /**
   * @return {@code VALID} and {@code SIGNED-OFF} projects for the {@code release}.
   */
  public List<Project> getValidProjects(@NonNull String release) {
    return getProjects(release, (submission) -> isValidState(submission.get("state").textValue()));
  }

  /**
   * @return all projects for the {@code release}.
   */
  public List<Project> getProjects(@NonNull String release) {
    return getProjects(release, (submission) -> true);
  }

  /**
   * Gets projects for a particular {@code release} from the releases submission endpoint, which satisfy the
   * {@code predicate}.
   * @param predicate on a {@code JsonNode} with the following structure:
   * 
   * <pre>
   * {
   * "name": "ICGC12",
   * "state": "COMPLETED",
   * "releaseDate": 1363655961497,
   * "dictionaryVersion": "0.6c",
   * "submissions": [
   *   {
   *     "projectKey": "TEST-CA",
   *     "projectName": null,
   *     "releaseName": null
   *   }
   * ]
   * }
   * </pre>
   */
  private List<Project> getProjects(String release, Predicate<JsonNode> predicate) {
    val response = getResponse(url + "/" + release.toUpperCase());
    val projects = ImmutableList.<Project> builder();
    for (val submission : response.get("submissions")) {
      log.debug("Processing submission: {}", submission);
      if (predicate.test(submission)) {
        log.debug("Adding matched submission to the projects collection...");
        projects.add(convertProject(submission));
      }
    }

    return projects.build();
  }

  private static boolean isValidState(String submissionState) {
    return SubmissionState.VALID.getName().equals(submissionState)
        || SubmissionState.SIGNED_OFF.getName().equals(submissionState);
  }

  private JsonNode getResponse(String requestUrl) {
    val restTemplate = new RestTemplate();

    ResponseEntity<JsonNode> response = null;
    try {
      response = restTemplate.exchange(
          requestUrl,
          HttpMethod.GET,
          new HttpEntity<JsonNode>(createHeaders(userName, password)),
          JsonNode.class);
    } catch (Exception e) {
      log.warn("Failed to get resource {}. Exception: {}", requestUrl, e);
      throw e;
    }

    return response.getBody();
  }

  private static Project convertProject(JsonNode submission) {
    val projectKey = submission.get("projectKey").textValue();
    val projectName = submission.get("projectName").textValue();
    val submissionState = submission.get("state").textValue();

    return new Project(projectKey, projectName, SubmissionState.valueOf(submissionState));
  }

  private static HttpHeaders createHeaders(String username, String password) {
    return new HttpHeaders() {

      {
        val auth = username + COLON + password;
        val encodedAuth = BaseEncoding.base64().encode(auth.getBytes(US_ASCII));
        val authHeader = "Basic " + encodedAuth;
        set(HttpHeaders.AUTHORIZATION, authHeader);
        set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
      }

    };
  }

}
