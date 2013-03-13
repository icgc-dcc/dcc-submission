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
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.icgc.dcc.portal.core.AllowedFields;
import org.icgc.dcc.portal.core.Types;
import org.icgc.dcc.portal.request.RequestSearchQuery;
import org.icgc.dcc.portal.results.GetResults;
import org.icgc.dcc.portal.results.SearchResults;
import org.icgc.dcc.portal.services.FilterService;

@Slf4j
public class GeneRepository implements IGeneRepository {

  private final static String INDEX = "icgc_test54"; // This should probably be set in a config

  private final static Types TYPE = Types.GENES;

  private static final AllowedFields ALLOWED_FIELDS = AllowedFields.GENES;

  private final Client client;

  private FilterBuilder filter;

  @Inject
  public GeneRepository(Client client) {
    this.client = client;
  }



  public final SearchResults search(final RequestSearchQuery requestSearchQuery) {
    this.filter = buildFilters(requestSearchQuery.getFilters());
    SearchRequestBuilder s = buildSearchRequest(requestSearchQuery);
    System.out.println(s);
    return new SearchResults(s.execute().actionGet(), requestSearchQuery);
  }

  public final GetResults get(final String id) {
    GetRequestBuilder g = buildGetRequest(id);
    return new GetResults(g.execute().actionGet());
  }

  private GetRequestBuilder buildGetRequest(String id) {
    return client.prepareGet(INDEX, TYPE.toString(), id).setFields(ALLOWED_FIELDS.toArray());
  }

  private SearchRequestBuilder buildSearchRequest(final RequestSearchQuery requestSearchQuery) {
    return client
        .prepareSearch(INDEX)
        .setTypes(TYPE.toString())
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(buildQuery())
        .setFilter(this.filter)
        .setFrom(requestSearchQuery.getFrom())
        .setSize(requestSearchQuery.getSize())
        .addSort(requestSearchQuery.getSort(), SortOrder.valueOf(requestSearchQuery.getOrder()))
        .addFields(ALLOWED_FIELDS.toArray())
        .addFacet(
            FacetBuilders.termsFacet("gene_type").field("gene_type")
                .facetFilter(setFacetFilter("gene_type", requestSearchQuery.getFilters())).size(Integer.MAX_VALUE)
                .global(true));
  }

  private QueryBuilder buildQuery() {
    return QueryBuilders //
        .nestedQuery("donor", //
            QueryBuilders.customScoreQuery(QueryBuilders.filteredQuery( //
                QueryBuilders.matchAllQuery(), //
                // this.filter//
                FilterBuilders.matchAllFilter()//
                )).script("doc['donor.somatic_mutation'].value") //
        ).scoreMode("total");
  }

  private FilterBuilder setFacetFilter(String name, JsonNode filter) {
    JsonNode temp = filter.deepCopy();
    ((ObjectNode) temp).remove(name);
    return buildFilters(temp);
  }

  private FilterBuilder buildFilters(JsonNode filters) {
    if (filters == null) {
      return FilterBuilders.matchAllFilter();
    } else {
      AndFilterBuilder geneFilters = FilterService.createGeneFilters(filters);
      if (filters.has("donor")) {
        geneFilters.add(FilterService.buildNestedFilter("donor", FilterService.createDonorFilters(filters)));
      }
      if (filters.has("mutation")) {
        geneFilters.add(FilterService.buildNestedFilter("mutation", FilterService.createMutationFilters(filters)));
      }
      return geneFilters;
    }
  }

}
