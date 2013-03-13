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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.get.GetRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.icgc.dcc.portal.core.AllowedFields;
import org.icgc.dcc.portal.core.Types;
import org.icgc.dcc.portal.request.RequestSearchQuery;
import org.icgc.dcc.portal.results.GetResults;
import org.icgc.dcc.portal.results.SearchResults;
import org.icgc.dcc.portal.services.FilterService;

@Slf4j
public class ProjectRepository implements IProjectRepository {

  // same now but might be different later
  private final static String INDEX = "icgc_test54"; // This should probably be set in a config

  // different
  private final static Types TYPE = Types.PROJECTS;

  // different
  private static final AllowedFields ALLOWED_FIELDS = AllowedFields.PROJECT;

  // same
  private final Client client;

  // same
  private FilterBuilder filter;

  // same
  @Inject
  public ProjectRepository(Client client) {
    this.client = client;
  }

  // same
  public final GetResults get(final String id) {
    GetRequestBuilder g = buildGetRequest(id);
    return new GetResults(g.execute().actionGet());
  }

  // same
  public final SearchResults search(final RequestSearchQuery requestSearchQuery) {
    this.filter = buildFilters(requestSearchQuery.getFilters());
    SearchRequestBuilder s = buildSearchRequest(requestSearchQuery);
    System.out.println(s);
    return new SearchResults(s.execute().actionGet(), requestSearchQuery);
  }

  // same
  private GetRequestBuilder buildGetRequest(String id) {
    return client.prepareGet(INDEX, TYPE.toString(), id).setFields(ALLOWED_FIELDS.toArray());
  }

  // different
  public final SearchRequestBuilder buildSearchRequest(final RequestSearchQuery requestSearchQuery) {
    return client
        // same
        .prepareSearch(INDEX)
        .setTypes(TYPE.toString())
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(buildQuery())
        .setFilter(this.filter)
        .setFrom(requestSearchQuery.getFrom())
        .setSize(requestSearchQuery.getSize())
        .addSort(requestSearchQuery.getSort(), SortOrder.valueOf(requestSearchQuery.getOrder()))
        .addFields(ALLOWED_FIELDS.toArray())
        // different
        .addFacet(
            FacetBuilders.termsFacet("project_name").field("project_name")
                .facetFilter(setFacetFilter("project_name", requestSearchQuery.getFilters())).size(Integer.MAX_VALUE)
                .global(true))
        .addFacet(
            FacetBuilders.termsFacet("primary_site").field("primary_site")
                .facetFilter(setFacetFilter("primary_site", requestSearchQuery.getFilters())).size(Integer.MAX_VALUE)
                .global(true))
        .addFacet(
            FacetBuilders.termsFacet("country").field("country")
                .facetFilter(setFacetFilter("country", requestSearchQuery.getFilters())).size(Integer.MAX_VALUE)
                .global(true))
        .addFacet(
            FacetBuilders.termsFacet("available_profiling_data").field("available_profiling_data")
                .facetFilter(setFacetFilter("available_profiling_data", requestSearchQuery.getFilters()))
                .size(Integer.MAX_VALUE).global(true));
  }

  // different
  private QueryBuilder buildQuery() {
    return QueryBuilders.matchAllQuery();
  }

  // same
  private FilterBuilder setFacetFilter(String name, JsonNode filter) {
    JsonNode temp = filter.deepCopy();
    ((ObjectNode) temp).remove(name);
    return buildFilters(temp);
  }

  // different
  private FilterBuilder buildFilters(JsonNode filters) {
    if (filters == null) {
      return FilterBuilders.matchAllFilter();
    } else {
      return FilterService.createProjectFilters(filters);
    }
  }
}
