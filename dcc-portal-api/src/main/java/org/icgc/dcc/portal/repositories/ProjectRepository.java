/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.repositories;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.sun.tools.javac.util.List;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.facet.FacetBuilders;
import org.icgc.dcc.portal.core.Types;
import org.icgc.dcc.portal.search.SearchQuery;

import java.util.ArrayList;

@Slf4j
public class ProjectRepository implements IProjectRepository {

  private final static String INDEX = "icgc_test54"; // This should probably be set in a config
  private final static Types TYPE = Types.PROJECTS;
  private static final String[] ALLOWED_FIELDS = ImmutableList.of("project_name", "primary_site", "country",
      "total_donor_count", "ssm_tested_donor_count", "cnsm_tested_donor_count", "exp_tested_donor_count",
      "meth_tested_donor_count", "pubmed_id").toArray(new String[9]);
  private final Client client;
  private FilterBuilder filter;
  private ObjectMapper mapper = new ObjectMapper();

  @Inject
  public ProjectRepository(Client client) {
    this.client = client;
  }

  // Returns one hit
  // @Override
  public final GetResponse getOne(final String id) {
    return client.prepareGet(INDEX, TYPE.toString(), id).execute().actionGet();
  }

  // Returns many hits
  // @Override
  public final SearchResponse getAll(final SearchQuery searchQuery) {
    this.filter = buildFilter(searchQuery.getFilters());
    SearchRequestBuilder s =
        client
            .prepareSearch(INDEX)
            .setTypes(TYPE.toString())
            .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
            .setQuery(buildQuery())
            .setFilter(this.filter)
            .setFrom(searchQuery.getFrom())
            .setSize(searchQuery.getSize())
            .addSort(searchQuery.getSort(), searchQuery.getOrder())
            .addFields(ALLOWED_FIELDS)
            .addFacet(
                FacetBuilders.termsFacet("project_name").field("project_name")
                    .facetFilter(setFacetFilter("project_name", searchQuery.getFilters())).size(Integer.MAX_VALUE)
                    .global(true))
            .addFacet(
                FacetBuilders.termsFacet("primary_site").field("primary_site")
                    .facetFilter(setFacetFilter("primary_site", searchQuery.getFilters())).size(Integer.MAX_VALUE)
                    .global(true))
            .addFacet(
                FacetBuilders.termsFacet("country").field("country")
                    .facetFilter(setFacetFilter("country", searchQuery.getFilters())).size(Integer.MAX_VALUE)
                    .global(true))
            .addFacet(
                FacetBuilders.termsFacet("available_profiling_data").field("available_profiling_data")
                    .facetFilter(setFacetFilter("available_profiling_data", searchQuery.getFilters()))
                    .size(Integer.MAX_VALUE).global(true));
    // System.out.println(s);
    return s.execute().actionGet();
  }

  private FilterBuilder setFacetFilter(String name, JsonNode filter) {
    JsonNode temp = filter.deepCopy();
    ((ObjectNode) temp).remove(name);
    return buildFilter(temp);
  }

  private FilterBuilder buildFilter(JsonNode filters) {
    if (filters == null) {
      return FilterBuilders.matchAllFilter();
    } else {
      return craftProjectFilters(filters);
    }
  }

  private AndFilterBuilder craftProjectFilters(JsonNode filters) {
    AndFilterBuilder projectAnd = FilterBuilders.andFilter();
    JsonNode project = filters;// .path("gene");
    System.out.println(project);
    for (String key : List.of("project_name", "primary_site", "country", "available_profiling_data")) {
      if (project.has(key)) {
        System.out.println(key + "here?");
        projectAnd.add(buildTermFilter(project, key));
      }
    }
    return projectAnd;
  }

  private FilterBuilder buildTermFilter(JsonNode json, String key) {
    FilterBuilder termFilter;
    if (json.get(key).isArray()) {
      ArrayList<String> terms = mapper.convertValue(json.get(key), new TypeReference<ArrayList<String>>() {});
      termFilter = FilterBuilders.termsFilter(key, terms);
    } else {
      String term = mapper.convertValue(json.get(key), String.class);
      termFilter = FilterBuilders.termFilter(key, term);
    }
    return termFilter;
  }

  private QueryBuilder buildQuery() {
    return QueryBuilders.matchAllQuery();
  }
}
