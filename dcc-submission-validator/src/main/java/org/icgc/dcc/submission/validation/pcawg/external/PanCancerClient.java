/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.pcawg.external;

import static org.icgc.dcc.common.core.util.FormatUtils.formatCount;
import static org.icgc.dcc.common.core.util.Jackson.DEFAULT;
import static org.icgc.dcc.common.core.util.Joiners.COMMA;

import java.net.URL;
import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

@Slf4j
@RequiredArgsConstructor
public class PanCancerClient {

  /**
   * Constants.
   */
  private static final String DEFAULT_PCAWG_URL = "http://pancancer.info";
  private static final String DEFAULT_PCAWG_INDEX_PATH = "elasticsearch/pcawg_es";
  private static final String DEFAULT_PCAWG_SEARCH_URL = DEFAULT_PCAWG_URL + "/" + DEFAULT_PCAWG_INDEX_PATH;

  @NonNull
  private final String searchUrl;

  public PanCancerClient() {
    this(DEFAULT_PCAWG_SEARCH_URL);
  }

  public Set<String> getProjects() {
    log.info("Searching projects...");
    val result = searchProjects("{aggs:{project:{terms:{field:\"dcc_project_code\",size:1000}}}}");
    log.info("Found {} projects", formatCount(result));

    val buckets = result.path("aggregations").path("project").path("buckets");

    val projects = Sets.<String> newTreeSet();
    for (val bucket : buckets) {
      val project = bucket.path("key").textValue();

      projects.add(project);
    }

    return ImmutableSet.copyOf(projects);
  }

  public Multimap<String, String> getProjectSampleIds() {
    log.info("Searching donors samples...");
    val result = searchDonors(
        "normal_alignment_status.dcc_specimen_type",
        "normal_alignment_status.submitter_sample_id",
        "tumor_alignment_status.dcc_specimen_type",
        "tumor_alignment_status.submitter_sample_id");
    log.info("Found donor samples");

    val hits = result.get("hits").get("hits");

    val builder = ImmutableMultimap.<String, String> builder();
    for (val hit : hits) {
      val projectKey = hit.get("_id").textValue().split("::")[0];

      val fields = hit.path("fields");
      for (val tumorSample : fields.path("tumor_alignment_status.submitter_sample_id")) {
        val sampleId = tumorSample.textValue();
        builder.put(projectKey, sampleId);
      }
      for (val normalSample : fields.path("normal_alignment_status.submitter_sample_id")) {
        val sampleId = normalSample.textValue();
        builder.put(projectKey, sampleId);
      }
    }

    val projectSamples = builder.build();
    log.info("Resolved {} samples in {} projects", formatCount(projectSamples.size()),
        formatCount(projectSamples.keySet()));

    return projectSamples;
  }

  private JsonNode searchProjects(String query) {
    return search("_search"
        + "?"
        + "size"
        + "="
        + "0"
        + "&"
        + "source"
        + "="
        + query);
  }

  private JsonNode searchDonors(String... fields) {
    return search("donor/_search"
        + "?"
        + "size"
        + "="
        + "10000"
        + "&"
        + "fields"
        + "="
        + COMMA.join(fields));
  }

  @SneakyThrows
  private JsonNode search(String path) {
    val url = new URL(searchUrl + "/" + path);

    log.info("Requesting '{}'...", url);
    return DEFAULT.readTree(url);
  }

}
