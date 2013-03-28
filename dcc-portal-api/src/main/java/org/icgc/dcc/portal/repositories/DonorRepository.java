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
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.facet.FacetBuilders;
import org.icgc.dcc.portal.models.Donor;
import org.icgc.dcc.portal.models.Gene;
import org.icgc.dcc.portal.models.Mutation;
import org.icgc.dcc.portal.request.RequestSearchQuery;
import org.icgc.dcc.portal.services.FilterService;

@Slf4j
public class DonorRepository extends BaseRepository {

  @Inject
  public DonorRepository(Client client) {
    super(client, Donor.INDEX, Donor.TYPE, Donor.FIELDS);
  }

  @Override
  QueryBuilder buildQuery() {

    return QueryBuilders //
        .nestedQuery(Gene.NAME, //
            QueryBuilders.customScoreQuery(QueryBuilders.filteredQuery( //
                QueryBuilders.matchAllQuery(), //
                getScoreFilters()
            )).script("doc['gene._summary._ssm_count'].value") //
        ).scoreMode("total");
    //return QueryBuilders.matchAllQuery();
  }

  @Override
  FilterBuilder buildScoreFilters(JsonNode filters) {
    if (filters.has(Gene.NAME)) {
      AndFilterBuilder scoreFilters = FilterBuilders.andFilter();
      scoreFilters
          .add(FilterService.buildNestedFilter(Gene.NAME, FilterService.buildAndFilters(Gene.FILTERS, filters.get(Gene.NAME))));
      return scoreFilters;
    }

    return FilterBuilders.matchAllFilter();
  }

  FilterBuilder buildFilters(JsonNode filters) {
    AndFilterBuilder donorFilters = FilterBuilders.andFilter();

    if (filters.has(Donor.NAME)) {
      donorFilters
          .add(FilterService.buildAndFilters(Donor.FILTERS, filters.get(Donor.NAME)));
    }
    if (filters.has(Gene.NAME)) {
      donorFilters
          .add(FilterService.buildNestedFilter(Gene.NAME, FilterService.buildAndFilters(Gene.FILTERS, filters.get(Gene.NAME))));
    }
    if (filters.has(Mutation.NAME)) {
      donorFilters.add(FilterService.buildNestedFilter(Mutation.NAME,
          FilterService.buildAndFilters(Mutation.FILTERS, filters.get(Mutation.NAME))));
    }
    return donorFilters;
  }

  SearchRequestBuilder addFacets(SearchRequestBuilder s, RequestSearchQuery requestSearchQuery) {
    for (String facet : Donor.FACETS.get("terms")) {
      s.addFacet(FacetBuilders.termsFacet(facet).field(facet)
          .facetFilter(setFacetFilter(Donor.NAME, facet, requestSearchQuery.getFilters())).size(Integer.MAX_VALUE).global(true));
    }
    return s;
  }
}
