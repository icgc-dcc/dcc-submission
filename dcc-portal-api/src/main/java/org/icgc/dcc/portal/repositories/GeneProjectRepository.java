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
import org.icgc.dcc.portal.models.Gene;
import org.icgc.dcc.portal.models.GeneProject;
import org.icgc.dcc.portal.models.Project;
import org.icgc.dcc.portal.request.RequestSearchQuery;
import org.icgc.dcc.portal.services.FilterService;

@Slf4j
public class GeneProjectRepository extends BaseRepository {

  @Inject
  public GeneProjectRepository(Client client) {
    super(client, GeneProject.INDEX, GeneProject.TYPE, GeneProject.FIELDS);
  }

  @Override
  SearchRequestBuilder addFacets(SearchRequestBuilder searchRequestBuilder, RequestSearchQuery requestSearchQuery) {
    return searchRequestBuilder;
  }

  @Override
  QueryBuilder buildQuery() {
    return QueryBuilders //
        .nestedQuery(Project.NAME, //
            QueryBuilders.customScoreQuery(QueryBuilders.filteredQuery( //
                QueryBuilders.matchAllQuery(), //
                getScoreFilters()
            )).script("doc['project.affected_donor_count'].value"
                //    "/doc['project.total_donor_count'].value"
            ) //
        ).scoreMode("total");
  }

  @Override
  FilterBuilder buildScoreFilters(JsonNode filters) {
    if (filters.has(Project.NAME)) {
      AndFilterBuilder score = FilterBuilders.andFilter();
      score
          .add(FilterService.buildNestedFilter(Project.NAME, FilterService.buildAndFilters(Project.FILTERS, filters.get(Project.NAME))));
      return score;
    }

    return FilterBuilders.matchAllFilter();
  }

  @Override
  FilterBuilder buildFilters(JsonNode filters) {
    AndFilterBuilder geneFilters = FilterBuilders.andFilter();
    System.out.println(geneFilters);
    if (filters.has(Gene.NAME)) {
      System.out.println("has gene");
      geneFilters
          .add(FilterService.buildAndFilters(Gene.FILTERS, filters.get(Gene.NAME)));
    }
    if (filters.has(Project.NAME)) {
      System.out.println("has project");
      geneFilters
          .add(FilterService.buildNestedFilter(Project.NAME, FilterService.buildAndFilters(Project.FILTERS, filters.get(Project.NAME))));
    }
    return geneFilters;
  }
}
